package br.com.corely.comercial.makeup;

import br.com.corely.comercial.attendance.Attendance;
import br.com.corely.comercial.attendance.AttendanceRepository;
import br.com.corely.comercial.attendance.AttendanceStatus;
import br.com.corely.comercial.booking.Booking;
import br.com.corely.comercial.booking.BookingRepository;
import br.com.corely.comercial.booking.BookingStatus;
import br.com.corely.comercial.classsession.ClassSession;
import br.com.corely.comercial.classsession.ClassSessionRepository;
import br.com.corely.comercial.classsession.SessionStatus;
import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.comercial.contractsnapshot.ContractSnapshotRepository;
import br.com.corely.comercial.makeup.dto.MakeUpCreditRequest;
import br.com.corely.comercial.schedule.Schedule;
import br.com.corely.comercial.schedule.ScheduleRepository;
import br.com.corely.comercial.scheduleslot.ScheduleSlot;
import br.com.corely.comercial.scheduleslot.ScheduleSlotRepository;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MakeUpServiceTest {

    @Autowired
    private MakeUpService makeUpService;

    @Autowired
    private MakeUpRepository makeUpRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentPlanRepository studentPlanRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractSnapshotRepository contractSnapshotRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Schedule schedule;
    private ScheduleSlot slot;
    private ClassSession session;
    private Student student;
    private Booking booking;
    private Attendance attendance;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        schedule = scheduleRepository.save(createSchedule("Morning Class"));
        slot = scheduleSlotRepository.save(createSlot(schedule, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));
        session = classSessionRepository.save(createSession(slot, LocalDate.of(2099, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));
        student = createAndSaveStudent("John Doe");
        createActiveStudentPlan(student);

        booking = createBooking(session, student);
        attendance = createAttendance(booking, AttendanceStatus.ABSENT);
    }

    @Test
    void findAll_shouldReturnPagedResults() {
        createMakeUpCredit(attendance, student);

        var page = makeUpService.findAll(null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findAll_shouldFilterByStudentId() {
        createMakeUpCredit(attendance, student);

        var secondStudent = createAndSaveStudent("Jane Doe");
        var secondBooking = createBooking(session, secondStudent);
        createActiveStudentPlan(secondStudent);
        var secondAttendance = createAttendance(secondBooking, AttendanceStatus.ABSENT);
        createMakeUpCredit(secondAttendance, secondStudent);

        var page = makeUpService.findAll(student.getId(), null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findAll_shouldFilterByStatus() {
        createMakeUpCredit(attendance, student);

        var page = makeUpService.findAll(null, MakeUpCreditStatus.AVAILABLE, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findById_shouldReturnMakeUpCredit() {
        var created = createMakeUpCredit(attendance, student);

        var response = makeUpService.findById(created.getId());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(created.getId());
        assertThat(response.getStudentName()).isEqualTo("John Doe");
        assertThat(response.getStatus()).isEqualTo(MakeUpCreditStatus.AVAILABLE);
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> makeUpService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("MakeUpCredit not found");
    }

    @Test
    void use_shouldCreateBookingAndMarkCreditAsUsed() {
        var credit = createMakeUpCredit(attendance, student);

        var targetSession = createFutureSession();
        var request = new MakeUpCreditRequest();
        request.setClassSessionId(targetSession.getId());

        var response = makeUpService.use(credit.getId(), request);

        assertThat(response.getStatus()).isEqualTo(MakeUpCreditStatus.USED);
        assertThat(response.getMakeUpBookingId()).isNotNull();

        var updatedCredit = makeUpRepository.findById(credit.getId()).orElseThrow();
        assertThat(updatedCredit.getStatus()).isEqualTo(MakeUpCreditStatus.USED);
        assertThat(updatedCredit.getMakeUpBooking()).isNotNull();
    }

    @Test
    void use_shouldThrowException_whenCreditNotFound() {
        var request = new MakeUpCreditRequest();
        request.setClassSessionId(UUID.randomUUID());

        assertThatThrownBy(() -> makeUpService.use(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("MakeUpCredit not found");
    }

    @Test
    void use_shouldThrowException_whenCreditExpired() {
        var credit = createMakeUpCredit(attendance, student);
        credit.setExpirationDate(LocalDate.now().minusDays(1));
        credit.setStatus(MakeUpCreditStatus.EXPIRED);
        makeUpRepository.save(credit);

        var request = new MakeUpCreditRequest();
        request.setClassSessionId(session.getId());

        assertThatThrownBy(() -> makeUpService.use(credit.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MakeUpCredit has expired");
    }

    @Test
    void use_shouldThrowException_whenCreditCancelled() {
        var credit = createMakeUpCredit(attendance, student);
        credit.setStatus(MakeUpCreditStatus.CANCELLED);
        makeUpRepository.save(credit);

        var request = new MakeUpCreditRequest();
        request.setClassSessionId(session.getId());

        assertThatThrownBy(() -> makeUpService.use(credit.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MakeUpCredit has been cancelled");
    }

    @Test
    void use_shouldThrowException_whenCreditAlreadyUsed() {
        var credit = createMakeUpCredit(attendance, student);
        credit.setStatus(MakeUpCreditStatus.USED);
        makeUpRepository.save(credit);

        var request = new MakeUpCreditRequest();
        request.setClassSessionId(session.getId());

        assertThatThrownBy(() -> makeUpService.use(credit.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MakeUpCredit has already been used");
    }

    @Test
    void use_shouldThrowException_whenSessionNotScheduled() {
        var credit = createMakeUpCredit(attendance, student);
        var targetSession = createFutureSession();
        targetSession.setStatus(SessionStatus.FINISHED);
        classSessionRepository.save(targetSession);

        var request = new MakeUpCreditRequest();
        request.setClassSessionId(targetSession.getId());

        assertThatThrownBy(() -> makeUpService.use(credit.getId(), request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void use_shouldThrowException_whenSessionAlreadyStarted() {
        var credit = createMakeUpCredit(attendance, student);
        var targetSession = classSessionRepository.save(createSession(slot, LocalDate.now(),
                LocalTime.now().minusHours(1), LocalTime.now()));

        var request = new MakeUpCreditRequest();
        request.setClassSessionId(targetSession.getId());

        assertThatThrownBy(() -> makeUpService.use(credit.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already started");
    }

    @Test
    void use_shouldThrowException_whenCreditInactive() {
        var credit = createMakeUpCredit(attendance, student);
        credit.setActive(false);
        makeUpRepository.save(credit);

        var request = new MakeUpCreditRequest();
        request.setClassSessionId(session.getId());

        assertThatThrownBy(() -> makeUpService.use(credit.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("MakeUpCredit is not active");
    }

    private MakeUpCredit createMakeUpCredit(Attendance att, Student stu) {
        var credit = new MakeUpCredit();
        credit.setStudio(studio);
        credit.setStudent(stu);
        credit.setOriginalAttendance(att);
        credit.setOriginalClassSession(session);
        credit.setExpirationDate(LocalDate.now().plusDays(30));
        credit.setStatus(MakeUpCreditStatus.AVAILABLE);
        credit.setActive(true);
        return makeUpRepository.save(credit);
    }

    private ClassSession createFutureSession() {
        var futureSlot = scheduleSlotRepository.save(createSlot(schedule, DayOfWeek.THURSDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 10));
        return classSessionRepository.save(createSession(futureSlot, LocalDate.of(2099, 8, 15),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "_" + studio.getId() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Studio createStudio(String name) {
        var studio = new Studio();
        studio.setName(name);
        studio.setActive(true);
        return studio;
    }

    private Schedule createSchedule(String name) {
        var schedule = new Schedule();
        schedule.setStudio(studio);
        schedule.setName(name);
        schedule.setActive(true);
        return schedule;
    }

    private ScheduleSlot createSlot(Schedule schedule, DayOfWeek dayOfWeek,
                                    LocalTime startTime, LocalTime endTime, int capacity) {
        var slot = new ScheduleSlot();
        slot.setStudio(studio);
        slot.setSchedule(schedule);
        slot.setDayOfWeek(dayOfWeek);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setCapacity(capacity);
        slot.setActive(true);
        return slot;
    }

    private ClassSession createSession(ScheduleSlot slot, LocalDate date,
                                       LocalTime startTime, LocalTime endTime) {
        var session = new ClassSession();
        session.setStudio(studio);
        session.setScheduleSlot(slot);
        session.setSessionDate(date);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setCapacity(slot.getCapacity());
        session.setBookedCount(0);
        session.setStatus(SessionStatus.SCHEDULED);
        session.setActive(true);
        return session;
    }

    private Student createAndSaveStudent(String name) {
        var student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setActive(true);
        return studentRepository.save(student);
    }

    private void createActiveStudentPlan(Student student) {
        createActiveStudentPlan(student, LocalDate.now().minusDays(30), null);
    }

    private void createActiveStudentPlan(Student student, LocalDate startDate, LocalDate endDate) {
        var snapshot = new ContractSnapshot();
        snapshot.setStudioId(studio.getId());
        snapshot.setPlanId(UUID.randomUUID());
        snapshot.setPlanVersion(1);
        snapshot.setPlanName("Test Plan");
        snapshot.setPlanPrice(java.math.BigDecimal.valueOf(100));
        snapshot.setPlanDuration(30);
        snapshot.setRules("{}");
        snapshot = contractSnapshotRepository.save(snapshot);

        var plan = new StudentPlan();
        plan.setStudio(studio);
        plan.setStudent(student);
        plan.setContractSnapshot(snapshot);
        plan.setStartDate(startDate);
        plan.setEndDate(endDate);
        plan.setStatus(StudentPlanStatus.ACTIVE);
        plan.setBookingBlocked(false);
        studentPlanRepository.save(plan);
    }

    private Booking createBooking(ClassSession classSession, Student student) {
        classSession.setBookedCount(classSession.getBookedCount() + 1);
        classSessionRepository.save(classSession);

        var booking = new Booking();
        booking.setStudio(studio);
        booking.setClassSession(classSession);
        booking.setStudent(student);
        booking.setBookingDateTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setActive(true);
        return bookingRepository.save(booking);
    }

    private Attendance createAttendance(Booking booking, AttendanceStatus status) {
        var attendance = new Attendance();
        attendance.setStudio(studio);
        attendance.setBooking(booking);
        attendance.setStatus(status);
        attendance.setActive(true);
        return attendanceRepository.save(attendance);
    }
}
