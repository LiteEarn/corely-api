package br.com.corely.comercial.waitlist;

import br.com.corely.comercial.booking.BookingRepository;
import br.com.corely.comercial.booking.BookingService;
import br.com.corely.comercial.classsession.ClassSession;
import br.com.corely.comercial.classsession.ClassSessionRepository;
import br.com.corely.comercial.classsession.SessionStatus;
import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.comercial.contractsnapshot.ContractSnapshotRepository;
import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.schedule.Schedule;
import br.com.corely.comercial.schedule.ScheduleRepository;
import br.com.corely.comercial.scheduleslot.ScheduleSlot;
import br.com.corely.comercial.scheduleslot.ScheduleSlotRepository;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.comercial.waitlist.dto.WaitListRequest;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WaitListPromotionServiceTest {

    @Autowired
    private WaitListPromotionService waitListPromotionService;

    @Autowired
    private WaitListService waitListService;

    @Autowired
    private WaitListRepository waitListRepository;

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
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ContractSnapshotRepository contractSnapshotRepository;

    @Autowired
    private StudentPlanRepository studentPlanRepository;

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
                LocalTime.of(8, 0), LocalTime.of(9, 0), 1));

        session = classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        student = createStudent("John Doe");
    }

    @Test
    void promoteIfNeeded_shouldCreateBookingAndMarkPromoted() {
        session.setBookedCount(0);
        classSessionRepository.save(session);

        createValidPlanForStudent(student, session.getSessionDate());

        var bookingRequest = new br.com.corely.comercial.booking.dto.BookingRequest();
        bookingRequest.setClassSessionId(session.getId());
        bookingRequest.setStudentId(student.getId());
        bookingService.create(bookingRequest);
        session.setBookedCount(1);
        classSessionRepository.save(session);

        var student2 = createStudent("Jane Doe");
        createValidPlanForStudent(student2, session.getSessionDate());
        var request = new WaitListRequest();
        request.setClassSessionId(session.getId());
        request.setStudentId(student2.getId());
        waitListService.create(request);

        var bookings = bookingRepository.findByClassSessionId(session.getId(),
                org.springframework.data.domain.PageRequest.of(0, 10));
        bookingService.delete(bookings.getContent().get(0).getId());

        waitListPromotionService.promoteIfNeeded(session.getId());

        assertThat(bookingRepository.existsByClassSessionIdAndStudentId(session.getId(), student2.getId())).isTrue();

        var waiting = waitListRepository.findActiveByClassSessionId(session.getId(),
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(waiting).isEmpty();
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

    private Student createStudent(String name) {
        var student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setActive(true);
        return studentRepository.save(student);
    }

    private void createValidPlanForStudent(Student student, LocalDate sessionDate) {
        var plan = new Plan();
        plan.setStudio(studio);
        plan.setName("Gold Plan");
        plan.setDescription("Gold Plan description");
        plan.setPrice(BigDecimal.valueOf(199));
        plan.setDuration(30);
        plan.setVersion(1);
        plan.setActive(true);
        plan = planRepository.save(plan);

        var snapshot = new ContractSnapshot();
        snapshot.setStudioId(studio.getId());
        snapshot.setPlanId(plan.getId());
        snapshot.setPlanVersion(1);
        snapshot.setPlanName("Gold Plan");
        snapshot.setPlanDescription("Gold Plan description");
        snapshot.setPlanPrice(BigDecimal.valueOf(199));
        snapshot.setPlanDuration(30);
        snapshot.setRules("{}");
        snapshot = contractSnapshotRepository.save(snapshot);

        var studentPlan = new StudentPlan();
        studentPlan.setStudio(studio);
        studentPlan.setStudent(student);
        studentPlan.setContractSnapshot(snapshot);
        studentPlan.setStartDate(sessionDate.minusDays(30));
        studentPlan.setEndDate(sessionDate.plusDays(30));
        studentPlan.setStatus(StudentPlanStatus.ACTIVE);
        studentPlan.setBookingBlocked(false);
        studentPlanRepository.save(studentPlan);
    }
}
