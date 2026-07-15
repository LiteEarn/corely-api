package br.com.corely.comercial.booking;

import br.com.corely.comercial.classsession.ClassSession;
import br.com.corely.comercial.classsession.ClassSessionRepository;
import br.com.corely.comercial.classsession.SessionStatus;
import br.com.corely.comercial.booking.dto.BookingRequest;
import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.comercial.contractsnapshot.ContractSnapshotRepository;
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
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

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
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Schedule schedule;
    private ScheduleSlot slot;
    private ClassSession session;
    private Student student;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        schedule = scheduleRepository.save(createSchedule("Morning Class"));
        slot = scheduleSlotRepository.save(createSlot(schedule, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));
        session = classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));
        student = createAndSaveStudent("John Doe");
        createActiveStudentPlan(student);
    }

    @Test
    void create_shouldPersistBooking() {
        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        var response = bookingService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getClassSessionId()).isEqualTo(session.getId());
        assertThat(response.getStudentId()).isEqualTo(student.getId());
        assertThat(response.getStudentName()).isEqualTo("John Doe");
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.getActive()).isTrue();

        var updatedSession = classSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(updatedSession.getBookedCount()).isEqualTo(1);
    }

    @Test
    void create_shouldThrowException_whenClassSessionNotFound() {
        var request = new BookingRequest();
        request.setClassSessionId(UUID.randomUUID());
        request.setStudentId(student.getId());

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("ClassSession not found");
    }

    @Test
    void create_shouldThrowException_whenClassSessionInactive() {
        session.setActive(false);
        classSessionRepository.save(session);

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ClassSession is not active");
    }

    @Test
    void create_shouldThrowException_whenClassSessionNotScheduled() {
        session.setStatus(SessionStatus.FINISHED);
        classSessionRepository.save(session);

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ClassSession status must be SCHEDULED");
    }

    @Test
    void create_shouldThrowException_whenClassSessionFull() {
        session.setCapacity(1);
        session.setBookedCount(1);
        classSessionRepository.save(session);

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ClassSession is full");
    }

    @Test
    void create_shouldThrowException_whenStudentNotFound() {
        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(UUID.randomUUID());

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Student not found");
    }

    @Test
    void create_shouldThrowException_whenStudentInactive() {
        student.setActive(false);
        studentRepository.save(student);

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Student is not active");
    }

    @Test
    void create_shouldThrowException_whenNoActivePlan() {
        var plan = studentPlanRepository.findByStudentIdAndStatus(student.getId(), StudentPlanStatus.ACTIVE);
        plan.ifPresent(p -> {
            p.setStatus(StudentPlanStatus.CANCELLED);
            studentPlanRepository.save(p);
        });

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Student does not have an active plan for this session date");
    }

    @Test
    void create_shouldThrowException_whenBookingBlocked() {
        var plan = studentPlanRepository.findByStudentIdAndStatus(student.getId(), StudentPlanStatus.ACTIVE);
        plan.ifPresent(p -> {
            p.setBookingBlocked(true);
            studentPlanRepository.save(p);
        });

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Student has booking blocked");
    }

    @Test
    void create_shouldThrowException_whenDuplicate() {
        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());
        bookingService.create(request);

        var duplicateRequest = new BookingRequest();
        duplicateRequest.setClassSessionId(session.getId());
        duplicateRequest.setStudentId(student.getId());

        assertThatThrownBy(() -> bookingService.create(duplicateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Student already has a booking");
    }

    @Test
    void findById_shouldReturnBooking() {
        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());
        var created = bookingService.create(request);

        var response = bookingService.findById(created.getId());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(created.getId());
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> bookingService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Booking not found");
    }

    @Test
    void findAll_shouldReturnPagedResults() {
        bookingService.create(createRequest(session.getId(), student.getId()));

        var secondStudent = createAndSaveStudent("Jane Doe");
        createActiveStudentPlan(secondStudent);
        bookingService.create(createRequest(session.getId(), secondStudent.getId()));

        var page = bookingService.findAll(null, null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void delete_shouldCancelBookingAndDecrementBookedCount() {
        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());
        var created = bookingService.create(request);

        bookingService.delete(created.getId());

        var booking = bookingRepository.findById(created.getId()).orElseThrow();
        assertThat(booking.getActive()).isFalse();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        var updatedSession = classSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(updatedSession.getBookedCount()).isZero();
    }

    @Test
    void delete_shouldNotMakeBookedCountNegative() {
        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());
        var created = bookingService.create(request);

        bookingService.delete(created.getId());
        bookingService.delete(created.getId());

        var updatedSession = classSessionRepository.findById(session.getId()).orElseThrow();
        assertThat(updatedSession.getBookedCount()).isZero();
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> bookingService.delete(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_shouldThrowException_whenPlanEndedBeforeSession() {
        studentPlanRepository.deleteAll();
        createActiveStudentPlan(student, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Student does not have an active plan for this session date");
    }

    @Test
    void create_shouldThrowException_whenPlanNotStartedYet() {
        studentPlanRepository.deleteAll();
        createActiveStudentPlan(student, LocalDate.of(2026, 9, 1), null);

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Student does not have an active plan for this session date");
    }

    @Test
    void cancel_shouldFreeCapacity() {
        session.setCapacity(1);
        classSessionRepository.save(session);

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());
        var created = bookingService.create(request);

        bookingService.delete(created.getId());

        var secondStudent = createAndSaveStudent("Jane Doe");
        createActiveStudentPlan(secondStudent);

        var secondRequest = new BookingRequest();
        secondRequest.setClassSessionId(session.getId());
        secondRequest.setStudentId(secondStudent.getId());
        var response = bookingService.create(secondRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(secondStudent.getId());
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

    private BookingRequest createRequest(UUID classSessionId, UUID studentId) {
        var request = new BookingRequest();
        request.setClassSessionId(classSessionId);
        request.setStudentId(studentId);
        return request;
    }
}
