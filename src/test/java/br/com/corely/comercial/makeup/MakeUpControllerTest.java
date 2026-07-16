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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MakeUpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentPlanRepository studentPlanRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private MakeUpRepository makeUpRepository;

    @Autowired
    private ContractSnapshotRepository contractSnapshotRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Schedule schedule;
    private ScheduleSlot slot;
    private ClassSession session;
    private Student student;
    private MakeUpCredit credit1;
    private MakeUpCredit credit2;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        schedule = scheduleRepository.save(createSchedule(studio, "Morning Class"));
        slot = scheduleSlotRepository.save(createSlot(studio, schedule, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));
        session = classSessionRepository.save(createSession(studio, slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        student = createAndSaveStudent("John Doe");
        createActiveStudentPlan(student);

        var booking1 = createBooking(session, student);
        var attendance1 = createAttendance(booking1, AttendanceStatus.ABSENT);
        credit1 = createMakeUpCredit(attendance1, student);

        var secondStudent = createAndSaveStudent("Jane Doe");
        createActiveStudentPlan(secondStudent);
        var booking2 = createBooking(session, secondStudent);
        var attendance2 = createAttendance(booking2, AttendanceStatus.ABSENT);
        credit2 = createMakeUpCredit(attendance2, secondStudent);
    }

    @Test
    void findAll_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/comercial/makeup-credits")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void findAll_shouldFilterByStudentId() throws Exception {
        mockMvc.perform(get("/comercial/makeup-credits")
                        .param("studentId", student.getId().toString())
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void findAll_shouldFilterByStatus() throws Exception {
        mockMvc.perform(get("/comercial/makeup-credits")
                        .param("status", "AVAILABLE")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void findById_shouldReturnMakeUpCredit() throws Exception {
        mockMvc.perform(get("/comercial/makeup-credits/{id}", credit1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(credit1.getId().toString()))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.studentName").value("John Doe"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/comercial/makeup-credits/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void use_shouldReturn200AndMarkCreditAsUsed() throws Exception {
        var targetSession = createFutureSession();
        var request = new MakeUpCreditRequest();
        request.setClassSessionId(targetSession.getId());

        mockMvc.perform(post("/comercial/makeup-credits/{id}/use", credit1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"))
                .andExpect(jsonPath("$.makeUpBookingId").isNotEmpty());
    }

    @Test
    void use_shouldReturn409_whenCreditExpired() throws Exception {
        credit1.setExpirationDate(LocalDate.now().minusDays(1));
        credit1.setStatus(MakeUpCreditStatus.EXPIRED);
        makeUpRepository.save(credit1);

        var request = new MakeUpCreditRequest();
        request.setClassSessionId(session.getId());

        mockMvc.perform(post("/comercial/makeup-credits/{id}/use", credit1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void use_shouldReturn409_whenCreditCancelled() throws Exception {
        credit1.setStatus(MakeUpCreditStatus.CANCELLED);
        makeUpRepository.save(credit1);

        var request = new MakeUpCreditRequest();
        request.setClassSessionId(session.getId());

        mockMvc.perform(post("/comercial/makeup-credits/{id}/use", credit1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void use_shouldReturn409_whenCreditAlreadyUsed() throws Exception {
        credit1.setStatus(MakeUpCreditStatus.USED);
        makeUpRepository.save(credit1);

        var request = new MakeUpCreditRequest();
        request.setClassSessionId(session.getId());

        mockMvc.perform(post("/comercial/makeup-credits/{id}/use", credit1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void use_shouldReturn409_whenSessionAlreadyStarted() throws Exception {
        var startedSession = classSessionRepository.save(createSession(studio, slot, LocalDate.now(),
                LocalTime.now().minusHours(1), LocalTime.now()));

        var request = new MakeUpCreditRequest();
        request.setClassSessionId(startedSession.getId());

        mockMvc.perform(post("/comercial/makeup-credits/{id}/use", credit1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void use_shouldReturn404_whenCreditNotFound() throws Exception {
        var request = new MakeUpCreditRequest();
        request.setClassSessionId(session.getId());

        mockMvc.perform(post("/comercial/makeup-credits/{id}/use", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "@test.com");
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

    private Schedule createSchedule(Studio studio, String name) {
        var schedule = new Schedule();
        schedule.setStudio(studio);
        schedule.setName(name);
        schedule.setActive(true);
        return schedule;
    }

    private ScheduleSlot createSlot(Studio studio, Schedule schedule, DayOfWeek dayOfWeek,
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

    private ClassSession createSession(Studio studio, ScheduleSlot slot, LocalDate date,
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

    private ClassSession createFutureSession() {
        var scheduleRepo = this.scheduleRepository;
        var slotRepo = this.scheduleSlotRepository;
        var futureSchedule = scheduleRepo.save(createSchedule(studio, "Future Class"));
        var futureSlot = slotRepo.save(createSlot(studio, futureSchedule, DayOfWeek.THURSDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 10));
        return classSessionRepository.save(createSession(studio, futureSlot, LocalDate.of(2099, 8, 15),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
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
}
