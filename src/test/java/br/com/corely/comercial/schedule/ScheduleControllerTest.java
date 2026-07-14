package br.com.corely.comercial.schedule;

import br.com.corely.comercial.schedule.dto.ScheduleRequest;
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

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScheduleControllerTest {

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
    private PasswordEncoder passwordEncoder;

    private Schedule activeSchedule;
    private Schedule inactiveSchedule;

    @BeforeEach
    void setUp() {
        var studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        activeSchedule = scheduleRepository.save(createSchedule(studio, "Morning Schedule", true));
        inactiveSchedule = scheduleRepository.save(createSchedule(studio, "Evening Schedule", false));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        var request = new ScheduleRequest();
        request.setName("New Schedule");
        request.setDescription("New schedule description");

        mockMvc.perform(post("/comercial/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("New Schedule"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_shouldReturn409_whenNameDuplicated() throws Exception {
        var request = new ScheduleRequest();
        request.setName("Morning Schedule");

        mockMvc.perform(post("/comercial/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn400_whenValidationFails() throws Exception {
        var request = new ScheduleRequest();
        request.setName("");

        mockMvc.perform(post("/comercial/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/comercial/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void findAll_withActiveParamTrue_shouldReturnOnlyActive() throws Exception {
        mockMvc.perform(get("/comercial/schedules?active=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Morning Schedule"));
    }

    @Test
    void findAll_withActiveParamFalse_shouldReturnOnlyInactive() throws Exception {
        mockMvc.perform(get("/comercial/schedules?active=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Evening Schedule"));
    }

    @Test
    void findAll_withNameParam_shouldFilterByName() throws Exception {
        mockMvc.perform(get("/comercial/schedules?name=Morning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Morning Schedule"));
    }

    @Test
    void findAll_withNameParam_shouldReturnEmpty_whenNoMatch() throws Exception {
        mockMvc.perform(get("/comercial/schedules?name=Nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void findById_shouldReturnSchedule() throws Exception {
        mockMvc.perform(get("/comercial/schedules/{id}", activeSchedule.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Morning Schedule"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/comercial/schedules/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        var request = new ScheduleRequest();
        request.setName("Updated Schedule");

        mockMvc.perform(put("/comercial/schedules/{id}", activeSchedule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Schedule"));
    }

    @Test
    void update_shouldReturn409_whenNameDuplicated() throws Exception {
        var request = new ScheduleRequest();
        request.setName("Evening Schedule");

        mockMvc.perform(put("/comercial/schedules/{id}", activeSchedule.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/comercial/schedules/{id}", activeSchedule.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn204_whenAlreadyInactive() throws Exception {
        mockMvc.perform(delete("/comercial/schedules/{id}", inactiveSchedule.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void readEndpoints_shouldBeAccessibleByReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);

        mockMvc.perform(get("/comercial/schedules"))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpoints_shouldBeAccessibleByReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);

        var request = new ScheduleRequest();
        request.setName("Receptionist Schedule");

        mockMvc.perform(post("/comercial/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void readEndpoints_shouldBeAccessibleByFinancial() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Financial"));
        authenticateAs(studio, UserRole.FINANCIAL);

        mockMvc.perform(get("/comercial/schedules"))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpoints_shouldBeForbiddenForFinancial() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Financial"));
        authenticateAs(studio, UserRole.FINANCIAL);

        var request = new ScheduleRequest();
        request.setName("Financial Schedule");

        mockMvc.perform(post("/comercial/schedules")
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

    private Schedule createSchedule(Studio studio, String name, boolean active) {
        var schedule = new Schedule();
        schedule.setStudio(studio);
        schedule.setName(name);
        schedule.setActive(active);
        return schedule;
    }
}
