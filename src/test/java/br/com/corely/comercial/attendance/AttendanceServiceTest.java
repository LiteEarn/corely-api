package br.com.corely.comercial.attendance;

import br.com.corely.comercial.attendance.dto.AttendanceRequest;
import br.com.corely.comercial.attendance.dto.AttendanceResponse;
import br.com.corely.comercial.attendance.dto.BulkAttendanceRequest;
import br.com.corely.comercial.booking.Booking;
import br.com.corely.comercial.booking.BookingRepository;
import br.com.corely.comercial.booking.BookingService;
import br.com.corely.comercial.booking.dto.BookingRequest;
import br.com.corely.comercial.booking.dto.BookingResponse;
import br.com.corely.comercial.classsession.ClassSession;
import br.com.corely.comercial.classsession.ClassSessionRepository;
import br.com.corely.comercial.classsession.SessionStatus;
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
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AttendanceServiceTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceRepository attendanceRepository;

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
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Schedule schedule;
    private ScheduleSlot slot;
    private ClassSession session;
    private Student student;
    private BookingResponse booking;

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
        booking = createBooking(session, student);
    }

    @Test
    void register_shouldPersistAttendance() {
        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null);

        var response = attendanceService.register(session.getId(), request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getClassSessionId()).isEqualTo(session.getId());
        assertThat(response.getBookingId()).isEqualTo(booking.getId());
        assertThat(response.getStudentId()).isEqualTo(student.getId());
        assertThat(response.getStudentName()).isEqualTo("John Doe");
        assertThat(response.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(response.getActive()).isTrue();
        assertThat(response.getCheckedInAt()).isNotNull();
    }

    @Test
    void register_shouldThrowException_whenSessionNotFound() {
        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null);

        assertThatThrownBy(() -> attendanceService.register(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("ClassSession not found");
    }

    @Test
    void register_shouldThrowException_whenSessionFinished() {
        session.setStatus(SessionStatus.FINISHED);
        classSessionRepository.save(session);

        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null);

        assertThatThrownBy(() -> attendanceService.register(session.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Attendance cannot be registered for a finished or cancelled session");
    }

    @Test
    void register_shouldThrowException_whenSessionCancelled() {
        session.setStatus(SessionStatus.CANCELLED);
        classSessionRepository.save(session);

        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null);

        assertThatThrownBy(() -> attendanceService.register(session.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Attendance cannot be registered for a finished or cancelled session");
    }

    @Test
    void register_shouldThrowException_whenBookingNotFound() {
        var request = new AttendanceRequest(UUID.randomUUID(), AttendanceStatus.PRESENT, null);

        assertThatThrownBy(() -> attendanceService.register(session.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Booking not found");
    }

    @Test
    void register_shouldThrowException_whenBookingNotActive() {
        var entity = bookingRepository.findById(booking.getId()).orElseThrow();
        entity.setActive(false);
        bookingRepository.save(entity);

        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null);

        assertThatThrownBy(() -> attendanceService.register(session.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Booking is not active");
    }

    @Test
    void register_shouldThrowException_whenBookingNotBelongToSession() {
        var otherSession = classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 2),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));

        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null);

        assertThatThrownBy(() -> attendanceService.register(otherSession.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Booking does not belong to this class session");
    }

    @Test
    void register_shouldUpdateExistingAttendance() {
        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, "Initial");
        attendanceService.register(session.getId(), request);

        var updateRequest = new AttendanceRequest(booking.getId(), AttendanceStatus.ABSENT, "Updated");
        var response = attendanceService.register(session.getId(), updateRequest);

        assertThat(response.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(response.getNotes()).isEqualTo("Updated");
    }

    @Test
    void register_shouldSetCheckedInAt_whenPresent() {
        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null);

        var response = attendanceService.register(session.getId(), request);

        assertThat(response.getCheckedInAt()).isNotNull();
    }

    @Test
    void register_shouldRecordAbsent() {
        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.ABSENT, "No show");

        var response = attendanceService.register(session.getId(), request);

        assertThat(response.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(response.getNotes()).isEqualTo("No show");
    }

    @Test
    void register_shouldRecordExcused() {
        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.EXCUSED, "Medical appointment");

        var response = attendanceService.register(session.getId(), request);

        assertThat(response.getStatus()).isEqualTo(AttendanceStatus.EXCUSED);
        assertThat(response.getNotes()).isEqualTo("Medical appointment");
    }

    @Test
    void findBySessionId_shouldReturnAttendances() {
        attendanceService.register(session.getId(), new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null));

        var result = attendanceService.findBySessionId(session.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    void findBySessionId_shouldThrowException_whenSessionNotFound() {
        assertThatThrownBy(() -> attendanceService.findBySessionId(UUID.randomUUID(), PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("ClassSession not found");
    }

    @Test
    void findByBookingId_shouldReturnAttendances() {
        attendanceService.register(session.getId(), new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null));

        var result = attendanceService.findByBookingId(booking.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findByStudentId_shouldReturnAttendances() {
        attendanceService.register(session.getId(), new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null));

        var result = attendanceService.findByStudentId(student.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void bulkSave_shouldSaveMultipleAttendances() {
        var otherStudent = createAndSaveStudent("Jane Doe");
        createActiveStudentPlan(otherStudent);
        var otherBooking = createBooking(session, otherStudent);

        var request = new BulkAttendanceRequest(session.getId(), List.of(
                new BulkAttendanceRequest.AttendanceItem(booking.getId(), true, null),
                new BulkAttendanceRequest.AttendanceItem(otherBooking.getId(), false, "Late")
        ));

        var response = attendanceService.bulkSave(request);

        assertThat(response.getSavedCount()).isEqualTo(2);
    }

    @Test
    void bulkSave_shouldThrowException_whenBookingNotBelongToSession() {
        var otherSession = classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 2),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));

        var otherStudent = createAndSaveStudent("Jane Doe");
        createActiveStudentPlan(otherStudent);
        var otherBooking = createBooking(otherSession, otherStudent);

        var request = new BulkAttendanceRequest(session.getId(), List.of(
                new BulkAttendanceRequest.AttendanceItem(otherBooking.getId(), true, null)
        ));

        assertThatThrownBy(() -> attendanceService.bulkSave(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void bulkSave_shouldThrowException_whenSessionFinished() {
        session.setStatus(SessionStatus.FINISHED);
        classSessionRepository.save(session);

        var request = new BulkAttendanceRequest(session.getId(), List.of(
                new BulkAttendanceRequest.AttendanceItem(booking.getId(), true, null)
        ));

        assertThatThrownBy(() -> attendanceService.bulkSave(request))
                .isInstanceOf(BusinessException.class);
    }

    private Studio createStudio(String name) {
        var studio = new Studio();
        studio.setName(name);
        studio.setActive(true);
        return studio;
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setStudio(studio);
        user.setName("Test User");
        user.setEmail("test@corely.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user = userRepository.save(user);

        var auth = UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
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
        plan.setStartDate(LocalDate.now().minusDays(30));
        plan.setEndDate(null);
        plan.setStatus(StudentPlanStatus.ACTIVE);
        plan.setBookingBlocked(false);
        studentPlanRepository.save(plan);
    }

    private BookingResponse createBooking(ClassSession session, Student student) {
        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());
        return bookingService.create(request);
    }
}
