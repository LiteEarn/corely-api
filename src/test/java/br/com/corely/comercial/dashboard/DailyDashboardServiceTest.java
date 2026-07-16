package br.com.corely.comercial.dashboard;

import br.com.corely.comercial.classsession.ClassSession;
import br.com.corely.comercial.classsession.ClassSessionRepository;
import br.com.corely.comercial.classsession.SessionStatus;
import br.com.corely.comercial.schedule.Schedule;
import br.com.corely.comercial.schedule.ScheduleRepository;
import br.com.corely.comercial.scheduleslot.ScheduleSlot;
import br.com.corely.comercial.scheduleslot.ScheduleSlotRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DailyDashboardServiceTest {

    @Autowired
    private DailyDashboardService dailyDashboardService;

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
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Schedule schedule;
    private ScheduleSlot slot;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        schedule = scheduleRepository.save(createSchedule("Morning Pilates"));
        slot = scheduleSlotRepository.save(createSlot(schedule, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));
    }

    @Test
    void getDailyDashboard_shouldReturnEmpty_whenNoSessions() {
        var response = dailyDashboardService.getDailyDashboard(LocalDate.of(2026, 8, 1));

        assertThat(response.getDataConsultada()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.getQuantidadeSessoes()).isZero();
        assertThat(response.getSessoes()).isEmpty();
    }

    @Test
    void getDailyDashboard_shouldReturnSessions() {
        var session = classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        var response = dailyDashboardService.getDailyDashboard(LocalDate.of(2026, 8, 1));

        assertThat(response.getDataConsultada()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.getQuantidadeSessoes()).isEqualTo(1);
        assertThat(response.getSessoes()).hasSize(1);
        assertThat(response.getSessoes().get(0).getId()).isEqualTo(session.getId());
        assertThat(response.getSessoes().get(0).getHorario()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.getSessoes().get(0).getProfessor()).isEqualTo("Morning Pilates");
        assertThat(response.getSessoes().get(0).getStatus()).isEqualTo(SessionStatus.SCHEDULED);
        assertThat(response.getSessoes().get(0).getCapacidade()).isEqualTo(10);
        assertThat(response.getSessoes().get(0).getVagasOcupadas()).isZero();
        assertThat(response.getSessoes().get(0).getVagasDisponiveis()).isEqualTo(10);
    }

    @Test
    void getDailyDashboard_shouldCountStatuses() {
        var s1 = createSession(slot, LocalDate.of(2026, 8, 1), LocalTime.of(8, 0), LocalTime.of(9, 0));
        var s2 = createSession(slot, LocalDate.of(2026, 8, 1), LocalTime.of(9, 0), LocalTime.of(10, 0));
        var s3 = createSession(slot, LocalDate.of(2026, 8, 1), LocalTime.of(10, 0), LocalTime.of(11, 0));
        var s4 = createSession(slot, LocalDate.of(2026, 8, 1), LocalTime.of(11, 0), LocalTime.of(12, 0));

        s2.setStatus(SessionStatus.IN_PROGRESS);
        s3.setStatus(SessionStatus.FINISHED);
        s4.setStatus(SessionStatus.CANCELLED);

        classSessionRepository.save(s1);
        classSessionRepository.save(s2);
        classSessionRepository.save(s3);
        classSessionRepository.save(s4);

        var response = dailyDashboardService.getDailyDashboard(LocalDate.of(2026, 8, 1));

        assertThat(response.getQuantidadeSessoes()).isEqualTo(4);
        assertThat(response.getSessoesIniciadas()).isEqualTo(1);
        assertThat(response.getSessoesFinalizadas()).isEqualTo(1);
        assertThat(response.getSessoesCanceladas()).isEqualTo(1);
    }

    @Test
    void getDailyDashboard_shouldCalculateCapacityAndBookings() {
        var session = createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0));
        session.setBookedCount(3);
        classSessionRepository.save(session);

        var response = dailyDashboardService.getDailyDashboard(LocalDate.of(2026, 8, 1));

        assertThat(response.getTotalVagas()).isEqualTo(10);
        assertThat(response.getVagasOcupadas()).isEqualTo(3);
        assertThat(response.getVagasLivres()).isEqualTo(7);
        assertThat(response.getSessoes().get(0).getVagasOcupadas()).isEqualTo(3);
        assertThat(response.getSessoes().get(0).getVagasDisponiveis()).isEqualTo(7);
    }

    @Test
    void getDailyDashboard_shouldNotIncludeSessionsFromOtherDates() {
        classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1), LocalTime.of(8, 0), LocalTime.of(9, 0)));
        classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 2), LocalTime.of(8, 0), LocalTime.of(9, 0)));

        var response = dailyDashboardService.getDailyDashboard(LocalDate.of(2026, 8, 1));

        assertThat(response.getQuantidadeSessoes()).isEqualTo(1);
    }

    @Test
    void getDailyDashboard_shouldNotIncludeInactiveSessions() {
        var session = classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));
        session.setActive(false);
        session.setStatus(SessionStatus.CANCELLED);
        classSessionRepository.save(session);

        var response = dailyDashboardService.getDailyDashboard(LocalDate.of(2026, 8, 1));

        assertThat(response.getQuantidadeSessoes()).isZero();
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
}