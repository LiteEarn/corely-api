package br.com.corely.comercial.booking;

import br.com.corely.comercial.classsession.ClassSession;
import br.com.corely.comercial.classsession.ClassSessionRepository;
import br.com.corely.comercial.classsession.SessionStatus;
import br.com.corely.comercial.booking.dto.*;
import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.comercial.contractsnapshot.ContractSnapshotRepository;
import br.com.corely.comercial.schedule.Schedule;
import br.com.corely.comercial.schedule.ScheduleRepository;
import br.com.corely.comercial.scheduleslot.ScheduleSlot;
import br.com.corely.comercial.scheduleslot.ScheduleSlotRepository;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private InstructorRepository instructorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Schedule schedule;
    private ScheduleSlot slot;
    private ClassSession session;
    private Student student;
    private Instructor instructor;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        instructor = instructorRepository.save(createInstructor("Test Instructor"));

        schedule = scheduleRepository.save(createSchedule(studio, "Schedule A"));
        slot = scheduleSlotRepository.save(createSlot(studio, schedule, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));
        session = classSessionRepository.save(createSession(studio, slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));
        student = createAndSaveStudent(studio, "Test Student");
        createActiveStudentPlan(studio, student);
    }

    @Test
    void create_shouldReturn201() throws Exception {
        var request = new BookingRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student.getId());

        mockMvc.perform(post("/comercial/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void findById_shouldReturnBooking() throws Exception {
        var booking = createBooking(session, student);

        mockMvc.perform(get("/comercial/bookings/{id}", booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value("Test Student"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/comercial/bookings/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        var booking = createBooking(session, student);

        mockMvc.perform(delete("/comercial/bookings/{id}", booking.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void confirm_shouldReturn200() throws Exception {
        var booking = createBooking(session, student);

        mockMvc.perform(put("/comercial/bookings/{id}/confirm", booking.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_shouldReturn200() throws Exception {
        var booking = createBooking(session, student);
        var cancelRequest = new CancelBookingRequest(CancelReason.STUDENT, "Student request");

        mockMvc.perform(put("/comercial/bookings/{id}/cancel", booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void reschedule_shouldReturn200() throws Exception {
        var booking = createBooking(session, student);
        var targetSession = classSessionRepository.save(createSession(studio, slot, LocalDate.of(2026, 8, 2),
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        var rescheduleRequest = new RescheduleBookingRequest(targetSession.getId());

        mockMvc.perform(put("/comercial/bookings/{id}/reschedule", booking.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rescheduleRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void noShow_shouldReturn200() throws Exception {
        var booking = createBooking(session, student);
        session.setStatus(SessionStatus.IN_PROGRESS);
        classSessionRepository.save(session);

        mockMvc.perform(put("/comercial/bookings/{id}/no-show", booking.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void complete_shouldReturn200() throws Exception {
        var booking = createBooking(session, student);
        session.setStatus(SessionStatus.IN_PROGRESS);
        classSessionRepository.save(session);
        session.setStatus(SessionStatus.FINISHED);
        classSessionRepository.save(session);

        mockMvc.perform(put("/comercial/bookings/{id}/complete", booking.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void findAgenda_shouldReturnResults() throws Exception {
        createBooking(session, student);

        mockMvc.perform(get("/comercial/bookings/agenda")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void findConflicts_shouldReturnEmpty_whenNoConflicts() throws Exception {
        var booking = createBooking(session, student);

        mockMvc.perform(get("/comercial/bookings/{id}/conflicts", booking.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void findAvailability_shouldReturnResults() throws Exception {
        mockMvc.perform(get("/comercial/bookings/availability")
                        .param("date", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getDashboard_shouldReturnMetrics() throws Exception {
        mockMvc.perform(get("/comercial/bookings/dashboard")
                        .param("date", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-08-01"));
    }

    @Test
    void timeBlock_create_shouldReturn201() throws Exception {
        var request = new TimeBlockRequest();
        request.setInstructorId(instructor.getId());
        request.setStartDateTime(LocalDate.now().plusDays(10).atTime(10, 0));
        request.setEndDateTime(LocalDate.now().plusDays(10).atTime(12, 0));
        request.setBlockType(BlockType.ADMIN);
        request.setReason("Test block");

        mockMvc.perform(post("/comercial/time-blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void timeBlock_findAll_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/comercial/time-blocks"))
                .andExpect(status().isOk());
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

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "_" + studio.getId() + "@controller.com");
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
        slot.setInstructor(instructor);
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

    private Instructor createInstructor(String name) {
        var instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName(name);
        instructor.setActive(true);
        return instructor;
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
        plan.setStartDate(LocalDate.now().minusDays(30));
        plan.setStatus(StudentPlanStatus.ACTIVE);
        plan.setBookingBlocked(false);
        studentPlanRepository.save(plan);
    }

    private Booking createBooking(ClassSession classSession, Student student) {
        var booking = new Booking();
        booking.setStudio(studio);
        booking.setClassSession(classSession);
        booking.setStudent(student);
        booking.setBookingDateTime(LocalDate.now().atStartOfDay());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setActive(true);

        classSession.setBookedCount(classSession.getBookedCount() + 1);
        classSessionRepository.save(classSession);

        return bookingRepository.save(booking);
    }
}
