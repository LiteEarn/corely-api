package br.com.corely.comercial.attendance;

import br.com.corely.comercial.attendance.dto.AttendanceRequest;
import br.com.corely.comercial.attendance.dto.BulkAttendanceRequest;
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
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private ClassSession session;
    private Student student;
    private BookingResponse booking;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        var today = LocalDate.now();
        var pastTime = LocalTime.now().minusHours(1);
        var schedule = scheduleRepository.save(createSchedule("Morning Class"));
        var slot = scheduleSlotRepository.save(createSlot(schedule, DayOfWeek.of(today.getDayOfWeek().getValue()),
                pastTime, pastTime.plusHours(1), 10));
        session = classSessionRepository.save(createSession(slot, today,
                pastTime, pastTime.plusHours(1)));
        student = createAndSaveStudent("John Doe");
        createActiveStudentPlan(student);
        booking = createBooking(session, student);
    }

    @Test
    void register_shouldReturn201() throws Exception {
        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null);

        mockMvc.perform(post("/comercial/attendances/sessions/{sessionId}", session.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PRESENT"))
                .andExpect(jsonPath("$.studentName").value("John Doe"));
    }

    @Test
    void register_shouldReturn404_whenSessionNotFound() throws Exception {
        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null);

        mockMvc.perform(post("/comercial/attendances/sessions/{sessionId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_shouldReturn409_whenBookingNotBelongToSession() throws Exception {
        var otherSession = classSessionRepository.save(createSession(
                scheduleSlotRepository.findAll().getFirst(), LocalDate.of(2026, 8, 2),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));

        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null);

        mockMvc.perform(post("/comercial/attendances/sessions/{sessionId}", otherSession.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturn409_whenSessionFinished() throws Exception {
        session.setStatus(SessionStatus.FINISHED);
        classSessionRepository.save(session);

        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null);

        mockMvc.perform(post("/comercial/attendances/sessions/{sessionId}", session.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void bulkSave_shouldReturn201() throws Exception {
        var request = new BulkAttendanceRequest(session.getId(), List.of(
                new BulkAttendanceRequest.AttendanceItem(booking.getId(), true, null)
        ));

        mockMvc.perform(post("/comercial/attendances/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.savedCount").value(1));
    }

    @Test
    void findBySessionId_shouldReturn200() throws Exception {
        attendanceRepository.save(createAttendance(session, booking, AttendanceStatus.PRESENT));

        mockMvc.perform(get("/comercial/attendances/sessions/{sessionId}", session.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void findByBookingId_shouldReturn200() throws Exception {
        attendanceRepository.save(createAttendance(session, booking, AttendanceStatus.PRESENT));

        mockMvc.perform(get("/comercial/attendances/bookings/{bookingId}", booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void findByStudentId_shouldReturn200() throws Exception {
        attendanceRepository.save(createAttendance(session, booking, AttendanceStatus.PRESENT));

        mockMvc.perform(get("/comercial/attendances/students/{studentId}", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void register_shouldBeForbiddenForFinancial() throws Exception {
        authenticateAs(studio, UserRole.FINANCIAL);

        var request = new AttendanceRequest(booking.getId(), AttendanceStatus.PRESENT, null);

        mockMvc.perform(post("/comercial/attendances/sessions/{sessionId}", session.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void bulkSave_shouldBeForbiddenForFinancial() throws Exception {
        authenticateAs(studio, UserRole.FINANCIAL);

        var request = new BulkAttendanceRequest(session.getId(), List.of(
                new BulkAttendanceRequest.AttendanceItem(booking.getId(), true, null)
        ));

        mockMvc.perform(post("/comercial/attendances/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void findBySessionId_shouldBeForbiddenForInstructor() throws Exception {
        authenticateAs(studio, UserRole.INSTRUCTOR);

        mockMvc.perform(get("/comercial/attendances/sessions/{sessionId}", session.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void findByBookingId_shouldBeForbiddenForInstructor() throws Exception {
        authenticateAs(studio, UserRole.INSTRUCTOR);

        mockMvc.perform(get("/comercial/attendances/bookings/{bookingId}", booking.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void findByStudentId_shouldBeForbiddenForInstructor() throws Exception {
        authenticateAs(studio, UserRole.INSTRUCTOR);

        mockMvc.perform(get("/comercial/attendances/students/{studentId}", student.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void findByScheduleAndDate_shouldReturn200() throws Exception {
        attendanceRepository.save(createAttendance(session, booking, AttendanceStatus.PRESENT));
        var schedule = scheduleRepository.findAll().getFirst();

        mockMvc.perform(get("/comercial/attendances/schedules/{scheduleId}/date/{date}",
                        schedule.getId(), session.getSessionDate()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
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

    private Attendance createAttendance(ClassSession session, BookingResponse booking, AttendanceStatus status) {
        var entity = bookingRepository.findById(booking.getId()).orElseThrow();
        var attendance = new Attendance();
        attendance.setStudio(studio);
        attendance.setBooking(entity);
        attendance.setStatus(status);
        attendance.setActive(true);
        if (status == AttendanceStatus.PRESENT) {
            attendance.setCheckedInAt(java.time.LocalDateTime.now());
        }
        return attendanceRepository.save(attendance);
    }
}
