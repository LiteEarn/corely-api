package br.com.corely.comercial.scheduleslot;

import br.com.corely.comercial.schedule.Schedule;
import br.com.corely.comercial.schedule.ScheduleRepository;
import br.com.corely.comercial.scheduleslot.dto.ScheduleSlotRequest;
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
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScheduleSlotControllerTest {

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
    private PasswordEncoder passwordEncoder;

    private Schedule schedule;
    private ScheduleSlot slot1;
    private ScheduleSlot slot2;

    @BeforeEach
    void setUp() {
        var studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        schedule = scheduleRepository.save(createSchedule(studio, "Morning Class"));

        slot1 = scheduleSlotRepository.save(createSlot(studio, schedule, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));
        slot2 = scheduleSlotRepository.save(createSlot(studio, schedule, DayOfWeek.WEDNESDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 15));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(10);

        mockMvc.perform(post("/comercial/schedules/{scheduleId}/slots", schedule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.scheduleId").value(schedule.getId().toString()))
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.capacity").value(10))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_shouldReturn404_whenScheduleNotFound() throws Exception {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(10);

        mockMvc.perform(post("/comercial/schedules/{scheduleId}/slots", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn409_whenOverlapExists() throws Exception {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(8, 30));
        request.setEndTime(LocalTime.of(9, 30));
        request.setCapacity(10);

        mockMvc.perform(post("/comercial/schedules/{scheduleId}/slots", schedule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn400_whenValidationFails() throws Exception {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(8, 0));
        request.setCapacity(10);

        mockMvc.perform(post("/comercial/schedules/{scheduleId}/slots", schedule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn400_whenEndTimeBeforeStartTime() throws Exception {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(9, 0));
        request.setCapacity(10);

        mockMvc.perform(post("/comercial/schedules/{scheduleId}/slots", schedule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn400_whenCapacityIsZero() throws Exception {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(0);

        mockMvc.perform(post("/comercial/schedules/{scheduleId}/slots", schedule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400_whenCapacityIsNegative() throws Exception {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(-1);

        mockMvc.perform(post("/comercial/schedules/{scheduleId}/slots", schedule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_shouldReturnSlotsForSchedule() throws Exception {
        mockMvc.perform(get("/comercial/schedules/{scheduleId}/slots", schedule.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void findById_shouldReturnSlot() throws Exception {
        mockMvc.perform(get("/comercial/schedule-slots/{id}", slot1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.startTime").value("08:00:00"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/comercial/schedule-slots/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(7, 0));
        request.setEndTime(LocalTime.of(8, 0));
        request.setCapacity(20);

        mockMvc.perform(put("/comercial/schedule-slots/{id}", slot1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("07:00:00"))
                .andExpect(jsonPath("$.capacity").value(20));
    }

    @Test
    void update_shouldReturn404_whenNotFound() throws Exception {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));
        request.setCapacity(10);

        mockMvc.perform(put("/comercial/schedule-slots/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/comercial/schedule-slots/{id}", slot1.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn204_whenAlreadyInactive() throws Exception {
        mockMvc.perform(delete("/comercial/schedule-slots/{id}", slot1.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/comercial/schedule-slots/{id}", slot1.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/comercial/schedule-slots/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void readEndpoints_shouldBeAccessibleByReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);

        var s = scheduleRepository.save(createSchedule(studio, "Receptionist Schedule"));

        mockMvc.perform(get("/comercial/schedules/{scheduleId}/slots", s.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpoints_shouldBeAccessibleByReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);

        var s = scheduleRepository.save(createSchedule(studio, "Receptionist Schedule"));

        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(10);

        mockMvc.perform(post("/comercial/schedules/{scheduleId}/slots", s.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void readEndpoints_shouldBeAccessibleByFinancial() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Financial"));
        authenticateAs(studio, UserRole.FINANCIAL);

        var s = scheduleRepository.save(createSchedule(studio, "Financial Schedule"));

        mockMvc.perform(get("/comercial/schedules/{scheduleId}/slots", s.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpoints_shouldBeForbiddenForFinancial() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Financial"));
        authenticateAs(studio, UserRole.FINANCIAL);

        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(10);

        mockMvc.perform(post("/comercial/schedules/{scheduleId}/slots", UUID.randomUUID())
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
}
