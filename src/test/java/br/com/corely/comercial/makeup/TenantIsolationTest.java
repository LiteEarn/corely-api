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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class TenantIsolationTest {

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

    @PersistenceContext
    private EntityManager entityManager;

    private Studio studioA;
    private Studio studioB;
    private Schedule scheduleA;
    private Schedule scheduleB;
    private ScheduleSlot slotA;
    private ScheduleSlot slotB;
    private ClassSession sessionA;
    private ClassSession sessionB;
    private Student studentA;
    private Student studentB;
    private MakeUpCredit creditA;
    private MakeUpCredit creditB;

    @BeforeEach
    void setUp() {
        studioA = studioRepository.save(createStudio("Studio A"));
        studioB = studioRepository.save(createStudio("Studio B"));

        createAndAuthenticateUser(studioA, UserRole.ADMIN);

        scheduleA = scheduleRepository.save(createSchedule(studioA, "Schedule A"));
        scheduleB = scheduleRepository.save(createSchedule(studioB, "Schedule B"));

        slotA = scheduleSlotRepository.save(createSlot(studioA, scheduleA, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));
        slotB = scheduleSlotRepository.save(createSlot(studioB, scheduleB, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));

        sessionA = classSessionRepository.save(createSession(studioA, slotA, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));
        sessionB = classSessionRepository.save(createSession(studioB, slotB, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        studentA = createAndSaveStudent(studioA, "Student A");
        studentB = createAndSaveStudent(studioB, "Student B");

        createActiveStudentPlan(studioA, studentA);
        createActiveStudentPlan(studioB, studentB);

        var bookingA = createBooking(studioA, sessionA, studentA);
        var bookingB = createBooking(studioB, sessionB, studentB);

        var attendanceA = createAttendance(studioA, bookingA, AttendanceStatus.ABSENT);
        var attendanceB = createAttendance(studioB, bookingB, AttendanceStatus.ABSENT);

        creditA = createMakeUpCredit(studioA, attendanceA, studentA, sessionA);
        creditB = createMakeUpCredit(studioB, attendanceB, studentB, sessionB);

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAll_shouldOnlyReturnCreditsFromCurrentTenant() throws Exception {
        mockMvc.perform(get("/comercial/makeup-credits")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void findById_shouldReturn404_whenCreditBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/comercial/makeup-credits/{id}", creditB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_shouldReturn200_whenCreditBelongsToCurrentTenant() throws Exception {
        mockMvc.perform(get("/comercial/makeup-credits/{id}", creditA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value("Student A"));
    }

    private User createAndAuthenticateUser(Studio studio, UserRole role) {
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
        return user;
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

    private Student createAndSaveStudent(Studio studio, String name) {
        var student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setActive(true);
        return studentRepository.save(student);
    }

    private void createActiveStudentPlan(Studio studio, Student student) {
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
        plan.setStartDate(LocalDate.now());
        plan.setStatus(StudentPlanStatus.ACTIVE);
        plan.setBookingBlocked(false);
        studentPlanRepository.save(plan);
    }

    private Booking createBooking(Studio studio, ClassSession classSession, Student student) {
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

    private Attendance createAttendance(Studio studio, Booking booking, AttendanceStatus status) {
        var attendance = new Attendance();
        attendance.setStudio(studio);
        attendance.setBooking(booking);
        attendance.setStatus(status);
        attendance.setActive(true);
        return attendanceRepository.save(attendance);
    }

    private MakeUpCredit createMakeUpCredit(Studio studio, Attendance att, Student stu, ClassSession session) {
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
