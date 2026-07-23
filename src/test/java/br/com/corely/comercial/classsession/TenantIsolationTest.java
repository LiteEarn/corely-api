package br.com.corely.comercial.classsession;

import br.com.corely.comercial.schedule.Schedule;
import br.com.corely.comercial.schedule.ScheduleRepository;
import br.com.corely.comercial.scheduleslot.ScheduleSlot;
import br.com.corely.comercial.scheduleslot.ScheduleSlotRepository;
import br.com.corely.comercial.classsession.dto.CancelSessionRequest;
import br.com.corely.comercial.classsession.dto.ClassSessionRequest;
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

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAll_shouldOnlyReturnSessionsFromCurrentTenant() throws Exception {
        mockMvc.perform(get("/comercial/class-sessions")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void findById_shouldReturn404_whenSessionBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/comercial/class-sessions/{id}", sessionB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_shouldReturn200_whenSessionBelongsToCurrentTenant() throws Exception {
        mockMvc.perform(get("/comercial/class-sessions/{id}", sessionA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionDate").value("2026-08-01"));
    }

    @Test
    void update_shouldReturn404_whenSessionBelongsToOtherTenant() throws Exception {
        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slotB.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        mockMvc.perform(put("/comercial/class-sessions/{id}", sessionB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn404_whenSessionBelongsToOtherTenant() throws Exception {
        mockMvc.perform(delete("/comercial/class-sessions/{id}", sessionB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_shouldReturn404_whenSessionBelongsToOtherTenant() throws Exception {
        var request = new CancelSessionRequest();
        request.setReason(SessionCancelReason.OTHER);

        mockMvc.perform(post("/comercial/class-sessions/{id}/cancel", sessionB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
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
