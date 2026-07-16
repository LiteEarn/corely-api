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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookingControllerTest {

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
    private ContractSnapshotRepository contractSnapshotRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Schedule schedule;
    private ScheduleSlot slot;
    private ClassSession session;
    private Student student;
    private Booking booking1;
    private Booking booking2;

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

        var secondStudent = createAndSaveStudent("Jane Doe");
        createActiveStudentPlan(secondStudent);

        booking1 = createBooking(session, student);
        booking2 = createBooking(session, secondStudent);
    }

    @Test
    void create_shouldReturn201() throws Exception {
        var thirdStudent = createAndSaveStudent("Bob Smith");
        createActiveStudentPlan(thirdStudent);

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(thirdStudent.getId());

        mockMvc.perform(post("/comercial/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.classSessionId").value(session.getId().toString()))
                .andExpect(jsonPath("$.studentId").value(thirdStudent.getId().toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_shouldReturn409_whenDuplicate() throws Exception {
        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        mockMvc.perform(post("/comercial/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn404_whenClassSessionNotFound() throws Exception {
        var request = new BookingRequest();
        request.setClassSessionId(UUID.randomUUID());
        request.setStudentId(student.getId());

        mockMvc.perform(post("/comercial/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn409_whenClassSessionFull() throws Exception {
        session.setCapacity(1);
        session.setBookedCount(1);
        classSessionRepository.save(session);

        var thirdStudent = createAndSaveStudent("Bob Smith");
        createActiveStudentPlan(thirdStudent);

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(thirdStudent.getId());

        mockMvc.perform(post("/comercial/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn409_whenStudentInactive() throws Exception {
        student.setActive(false);
        studentRepository.save(student);

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        mockMvc.perform(post("/comercial/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn409_whenNoActivePlan() throws Exception {
        var plan = studentPlanRepository.findByStudentIdAndStatus(student.getId(), StudentPlanStatus.ACTIVE);
        plan.ifPresent(p -> {
            p.setStatus(StudentPlanStatus.CANCELLED);
            studentPlanRepository.save(p);
        });

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        mockMvc.perform(post("/comercial/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn409_whenBookingBlocked() throws Exception {
        var plan = studentPlanRepository.findByStudentIdAndStatus(student.getId(), StudentPlanStatus.ACTIVE);
        plan.ifPresent(p -> {
            p.setBookingBlocked(true);
            studentPlanRepository.save(p);
        });

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        mockMvc.perform(post("/comercial/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn409_whenPlanEndedBeforeSession() throws Exception {
        var newStudent = createAndSaveStudent("Plan Ended");
        createActiveStudentPlan(newStudent, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(newStudent.getId());

        mockMvc.perform(post("/comercial/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn409_whenPlanNotStartedYet() throws Exception {
        var newStudent = createAndSaveStudent("Plan Future");
        createActiveStudentPlan(newStudent, LocalDate.of(2026, 9, 1), null);

        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(newStudent.getId());

        mockMvc.perform(post("/comercial/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void findAll_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/comercial/bookings")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void findById_shouldReturnBooking() throws Exception {
        mockMvc.perform(get("/comercial/bookings/{id}", booking1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(booking1.getId().toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/comercial/bookings/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/comercial/bookings/{id}", booking1.getId()))
                .andExpect(status().isNoContent());

        var updatedSession = classSessionRepository.findById(session.getId()).orElseThrow();
        assert updatedSession.getBookedCount() >= 0;
    }

    @Test
    void delete_shouldReturn204_whenAlreadyCancelled() throws Exception {
        mockMvc.perform(delete("/comercial/bookings/{id}", booking1.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/comercial/bookings/{id}", booking1.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/comercial/bookings/{id}", UUID.randomUUID()))
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

    private Booking createBooking(ClassSession session, Student student) {
        var booking = new Booking();
        booking.setStudio(studio);
        booking.setClassSession(session);
        booking.setStudent(student);
        booking.setBookingDateTime(LocalDate.now().atStartOfDay());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setActive(true);
        return bookingRepository.save(booking);
    }
}
