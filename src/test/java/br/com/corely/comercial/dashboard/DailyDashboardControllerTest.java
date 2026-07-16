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
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DailyDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private br.com.corely.comercial.classsession.ClassSessionRepository classSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Schedule schedule;
    private ScheduleSlot slot;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        schedule = scheduleRepository.save(createSchedule(studio, "Morning Pilates"));
        slot = scheduleSlotRepository.save(createSlot(studio, schedule, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));
    }

    @Test
    void getDailyDashboard_shouldReturn200() throws Exception {
        mockMvc.perform(get("/comercial/dashboard/daily")
                        .param("date", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataConsultada").value("2026-08-01"))
                .andExpect(jsonPath("$.quantidadeSessoes").value(0));
    }

    @Test
    void getDailyDashboard_shouldReturnSessions() throws Exception {
        var session = classSessionRepository.save(createSession(studio, slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        mockMvc.perform(get("/comercial/dashboard/daily")
                        .param("date", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataConsultada").value("2026-08-01"))
                .andExpect(jsonPath("$.quantidadeSessoes").value(1))
                .andExpect(jsonPath("$.sessoes[0].id").value(session.getId().toString()))
                .andExpect(jsonPath("$.sessoes[0].horario").value("08:00:00"))
                .andExpect(jsonPath("$.sessoes[0].professor").value("Morning Pilates"))
                .andExpect(jsonPath("$.sessoes[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.sessoes[0].capacidade").value(10))
                .andExpect(jsonPath("$.sessoes[0].vagasOcupadas").value(0))
                .andExpect(jsonPath("$.sessoes[0].vagasDisponiveis").value(10));
    }

    @Test
    void getDailyDashboard_shouldUseToday_whenNoDateParam() throws Exception {
        mockMvc.perform(get("/comercial/dashboard/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataConsultada").isNotEmpty());
    }

    @Test
    void getDailyDashboard_shouldBeAccessibleByReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);

        mockMvc.perform(get("/comercial/dashboard/daily")
                        .param("date", "2026-08-01"))
                .andExpect(status().isOk());
    }

    @Test
    void getDailyDashboard_shouldBeAccessibleByFinancial() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Financial"));
        authenticateAs(studio, UserRole.FINANCIAL);

        mockMvc.perform(get("/comercial/dashboard/daily")
                        .param("date", "2026-08-01"))
                .andExpect(status().isOk());
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
}