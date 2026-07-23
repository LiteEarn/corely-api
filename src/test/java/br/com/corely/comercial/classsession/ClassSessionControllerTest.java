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
class ClassSessionControllerTest {

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

    private Studio studio;
    private Schedule schedule;
    private ScheduleSlot slot;
    private ClassSession session1;
    private ClassSession session2;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        schedule = scheduleRepository.save(createSchedule(studio, "Morning Class"));
        slot = scheduleSlotRepository.save(createSlot(studio, schedule, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));

        session1 = classSessionRepository.save(createSession(studio, slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));
        session2 = classSessionRepository.save(createSession(studio, slot, LocalDate.of(2026, 8, 2),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 3));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        mockMvc.perform(post("/comercial/class-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.scheduleSlotId").value(slot.getId().toString()))
                .andExpect(jsonPath("$.sessionDate").value("2026-08-03"))
                .andExpect(jsonPath("$.capacity").value(10))
                .andExpect(jsonPath("$.bookedCount").value(0))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_shouldReturn404_whenScheduleSlotNotFound() throws Exception {
        var request = new ClassSessionRequest();
        request.setScheduleSlotId(UUID.randomUUID());
        request.setSessionDate(LocalDate.of(2026, 8, 3));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        mockMvc.perform(post("/comercial/class-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn409_whenScheduleSlotInactive() throws Exception {
        slot.setActive(false);
        scheduleSlotRepository.save(slot);

        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 3));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        mockMvc.perform(post("/comercial/class-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn409_whenDuplicate() throws Exception {
        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        mockMvc.perform(post("/comercial/class-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn409_whenEndTimeEqualsStartTime() throws Exception {
        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 3));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(8, 0));

        mockMvc.perform(post("/comercial/class-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void findAll_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/comercial/class-sessions")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void findAll_shouldFilterByScheduleSlotId() throws Exception {
        mockMvc.perform(get("/comercial/class-sessions")
                        .param("scheduleSlotId", slot.getId().toString())
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void findById_shouldReturnSession() throws Exception {
        mockMvc.perform(get("/comercial/class-sessions/{id}", session1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionDate").value("2026-08-01"))
                .andExpect(jsonPath("$.startTime").value("08:00:00"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/comercial/class-sessions/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(7, 0));
        request.setEndTime(LocalTime.of(8, 0));

        mockMvc.perform(put("/comercial/class-sessions/{id}", session1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("07:00:00"))
                .andExpect(jsonPath("$.endTime").value("08:00:00"));
    }

    @Test
    void update_shouldReturn409_whenSessionFinished() throws Exception {
        session1.setStatus(SessionStatus.FINISHED);
        classSessionRepository.save(session1);

        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        mockMvc.perform(put("/comercial/class-sessions/{id}", session1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void update_shouldReturn404_whenNotFound() throws Exception {
        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        mockMvc.perform(put("/comercial/class-sessions/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn409_whenScheduleSlotInactive() throws Exception {
        var inactiveSlot = scheduleSlotRepository.save(createSlot(studio, schedule, DayOfWeek.FRIDAY,
                LocalTime.of(14, 0), LocalTime.of(15, 0), 5));
        inactiveSlot.setActive(false);
        scheduleSlotRepository.save(inactiveSlot);

        var request = new ClassSessionRequest();
        request.setScheduleSlotId(inactiveSlot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 7));
        request.setStartTime(LocalTime.of(14, 0));
        request.setEndTime(LocalTime.of(15, 0));

        mockMvc.perform(put("/comercial/class-sessions/{id}", session1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/comercial/class-sessions/{id}", session1.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn204_whenAlreadyInactive() throws Exception {
        mockMvc.perform(delete("/comercial/class-sessions/{id}", session1.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/comercial/class-sessions/{id}", session1.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/comercial/class-sessions/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void readEndpoints_shouldBeAccessibleByReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);

        mockMvc.perform(get("/comercial/class-sessions")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpoints_shouldBeAccessibleByReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);
        var s = scheduleRepository.save(createSchedule(studio, "Receptionist Schedule"));
        var sl = scheduleSlotRepository.save(createSlot(studio, s, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));

        var request = new ClassSessionRequest();
        request.setScheduleSlotId(sl.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 3));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        mockMvc.perform(post("/comercial/class-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void readEndpoints_shouldBeAccessibleByFinancial() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Financial"));
        authenticateAs(studio, UserRole.FINANCIAL);

        mockMvc.perform(get("/comercial/class-sessions")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpoints_shouldBeForbiddenForFinancial() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Financial"));
        authenticateAs(studio, UserRole.FINANCIAL);

        var request = new ClassSessionRequest();
        request.setScheduleSlotId(UUID.randomUUID());
        request.setSessionDate(LocalDate.of(2026, 8, 3));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        mockMvc.perform(post("/comercial/class-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void start_shouldReturn200AndChangeStatus() throws Exception {
        mockMvc.perform(post("/comercial/class-sessions/{id}/start", session1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void start_shouldReturn409_whenAlreadyInProgress() throws Exception {
        session1.setStatus(SessionStatus.IN_PROGRESS);
        classSessionRepository.save(session1);

        mockMvc.perform(post("/comercial/class-sessions/{id}/start", session1.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void start_shouldReturn409_whenFinished() throws Exception {
        session1.setStatus(SessionStatus.FINISHED);
        classSessionRepository.save(session1);

        mockMvc.perform(post("/comercial/class-sessions/{id}/start", session1.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void start_shouldReturn409_whenCancelled() throws Exception {
        session1.setStatus(SessionStatus.CANCELLED);
        classSessionRepository.save(session1);

        mockMvc.perform(post("/comercial/class-sessions/{id}/start", session1.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void start_shouldReturn409_whenInactive() throws Exception {
        session1.setActive(false);
        classSessionRepository.save(session1);

        mockMvc.perform(post("/comercial/class-sessions/{id}/start", session1.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void finish_shouldReturn200AndChangeStatus() throws Exception {
        session1.setStatus(SessionStatus.IN_PROGRESS);
        classSessionRepository.save(session1);

        mockMvc.perform(post("/comercial/class-sessions/{id}/finish", session1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void finish_shouldReturn409_whenStillScheduled() throws Exception {
        mockMvc.perform(post("/comercial/class-sessions/{id}/finish", session1.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void finish_shouldReturn409_whenAlreadyFinished() throws Exception {
        session1.setStatus(SessionStatus.FINISHED);
        classSessionRepository.save(session1);

        mockMvc.perform(post("/comercial/class-sessions/{id}/finish", session1.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void finish_shouldReturn409_whenCancelled() throws Exception {
        session1.setStatus(SessionStatus.CANCELLED);
        classSessionRepository.save(session1);

        mockMvc.perform(post("/comercial/class-sessions/{id}/finish", session1.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void start_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(post("/comercial/class-sessions/{id}/start", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void finish_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(post("/comercial/class-sessions/{id}/finish", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_shouldReturn200() throws Exception {
        var request = new CancelSessionRequest();
        request.setReason(SessionCancelReason.INSTRUCTOR_UNAVAILABLE);
        request.setDescription("Instructor sick");

        mockMvc.perform(post("/comercial/class-sessions/{id}/cancel", session1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("INSTRUCTOR_UNAVAILABLE"))
                .andExpect(jsonPath("$.cancelDescription").value("Instructor sick"))
                .andExpect(jsonPath("$.cancelledBy").isNotEmpty())
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());
    }

    @Test
    void cancel_shouldReturn409_whenSessionNotScheduled() throws Exception {
        session1.setStatus(SessionStatus.IN_PROGRESS);
        classSessionRepository.save(session1);

        var request = new CancelSessionRequest();
        request.setReason(SessionCancelReason.OTHER);

        mockMvc.perform(post("/comercial/class-sessions/{id}/cancel", session1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void cancel_shouldReturn404_whenNotFound() throws Exception {
        var request = new CancelSessionRequest();
        request.setReason(SessionCancelReason.OTHER);

        mockMvc.perform(post("/comercial/class-sessions/{id}/cancel", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_shouldBeForbiddenForFinancial() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Financial"));
        authenticateAs(studio, UserRole.FINANCIAL);

        var request = new CancelSessionRequest();
        request.setReason(SessionCancelReason.OTHER);

        mockMvc.perform(post("/comercial/class-sessions/{id}/cancel", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
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
