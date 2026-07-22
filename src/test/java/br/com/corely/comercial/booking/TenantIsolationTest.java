package br.com.corely.comercial.booking;

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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
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
    private ContractSnapshotRepository contractSnapshotRepository;

    @Autowired
    private InstructorRepository instructorRepository;

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
    private Booking bookingA;
    private Booking bookingB;
    private Instructor instructorA;
    private Instructor instructorB;

    @BeforeEach
    void setUp() {
        studioA = studioRepository.save(createStudio("Studio A"));
        studioB = studioRepository.save(createStudio("Studio B"));

        createAndAuthenticateUser(studioA, UserRole.ADMIN);

        instructorA = instructorRepository.save(createInstructor(studioA, "Instructor A"));
        instructorB = instructorRepository.save(createInstructor(studioB, "Instructor B"));

        scheduleA = scheduleRepository.save(createSchedule(studioA, "Schedule A"));
        scheduleB = scheduleRepository.save(createSchedule(studioB, "Schedule B"));

        slotA = scheduleSlotRepository.save(createSlot(studioA, scheduleA, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10, instructorA));
        slotB = scheduleSlotRepository.save(createSlot(studioB, scheduleB, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10, instructorB));

        sessionA = classSessionRepository.save(createSession(studioA, slotA, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));
        sessionB = classSessionRepository.save(createSession(studioB, slotB, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        studentA = createAndSaveStudent(studioA, "Student A");
        studentB = createAndSaveStudent(studioB, "Student B");

        createActiveStudentPlan(studioA, studentA);
        createActiveStudentPlan(studioB, studentB);

        bookingA = createBooking(studioA, sessionA, studentA);
        bookingB = createBooking(studioB, sessionB, studentB);

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAll_shouldOnlyReturnBookingsFromCurrentTenant() throws Exception {
        mockMvc.perform(get("/comercial/bookings")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void findById_shouldReturn404_whenBookingBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/comercial/bookings/{id}", bookingB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_shouldReturn200_whenBookingBelongsToCurrentTenant() throws Exception {
        mockMvc.perform(get("/comercial/bookings/{id}", bookingA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value("Student A"));
    }

    @Test
    void delete_shouldReturn404_whenBookingBelongsToOtherTenant() throws Exception {
        mockMvc.perform(delete("/comercial/bookings/{id}", bookingB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirm_shouldReturn404_whenBookingBelongsToOtherTenant() throws Exception {
        mockMvc.perform(put("/comercial/bookings/{id}/confirm", bookingB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_shouldReturn404_whenBookingBelongsToOtherTenant() throws Exception {
        var cancelRequest = new br.com.corely.comercial.booking.dto.CancelBookingRequest(
                br.com.corely.comercial.booking.CancelReason.OTHER, null);
        mockMvc.perform(put("/comercial/bookings/{id}/cancel", bookingB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void reschedule_shouldReturn404_whenBookingBelongsToOtherTenant() throws Exception {
        var rescheduleRequest = new br.com.corely.comercial.booking.dto.RescheduleBookingRequest(sessionA.getId());
        mockMvc.perform(put("/comercial/bookings/{id}/reschedule", bookingB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rescheduleRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void noShow_shouldReturn404_whenBookingBelongsToOtherTenant() throws Exception {
        mockMvc.perform(put("/comercial/bookings/{id}/no-show", bookingB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void complete_shouldReturn404_whenBookingBelongsToOtherTenant() throws Exception {
        mockMvc.perform(put("/comercial/bookings/{id}/complete", bookingB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findConflicts_shouldReturn404_whenBookingBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/comercial/bookings/{id}/conflicts", bookingB.getId()))
                .andExpect(status().isNotFound());
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
                                    LocalTime startTime, LocalTime endTime, int capacity,
                                    Instructor instructor) {
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

    private Instructor createInstructor(Studio studio, String name) {
        var instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName(name);
        instructor.setActive(true);
        return instructor;
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private Booking createBooking(Studio studio, ClassSession session, Student student) {
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
