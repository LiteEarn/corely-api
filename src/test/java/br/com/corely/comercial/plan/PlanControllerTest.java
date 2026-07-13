package br.com.corely.comercial.plan;

import br.com.corely.comercial.plan.dto.PlanRequest;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Plan activePlan;
    private Plan inactivePlan;

    @BeforeEach
    void setUp() {
        var studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        activePlan = planRepository.save(createPlan(studio, "Gold Plan", BigDecimal.valueOf(100), 30, true));
        inactivePlan = planRepository.save(createPlan(studio, "Silver Plan", BigDecimal.valueOf(50), 15, false));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        var request = new PlanRequest();
        request.setName("New Plan");
        request.setDescription("New plan description");
        request.setPrice(BigDecimal.valueOf(150));
        request.setDuration(45);

        mockMvc.perform(post("/comercial/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("New Plan"))
                .andExpect(jsonPath("$.price").value(150))
                .andExpect(jsonPath("$.duration").value(45))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void create_shouldReturn409_whenNameDuplicated() throws Exception {
        var request = new PlanRequest();
        request.setName("Gold Plan");
        request.setPrice(BigDecimal.valueOf(200));
        request.setDuration(30);

        mockMvc.perform(post("/comercial/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn400_whenValidationFails() throws Exception {
        var request = new PlanRequest();
        request.setName("");

        mockMvc.perform(post("/comercial/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/comercial/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void findAll_withActiveParamTrue_shouldReturnOnlyActive() throws Exception {
        mockMvc.perform(get("/comercial/plans?active=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Gold Plan"));
    }

    @Test
    void findAll_withActiveParamFalse_shouldReturnOnlyInactive() throws Exception {
        mockMvc.perform(get("/comercial/plans?active=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Silver Plan"));
    }

    @Test
    void findAll_withNameParam_shouldFilterByName() throws Exception {
        mockMvc.perform(get("/comercial/plans?name=Gold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Gold Plan"));
    }

    @Test
    void findAll_withNameParam_shouldReturnEmpty_whenNoMatch() throws Exception {
        mockMvc.perform(get("/comercial/plans?name=Nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void findById_shouldReturnPlan() throws Exception {
        mockMvc.perform(get("/comercial/plans/{id}", activePlan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gold Plan"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/comercial/plans/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        var request = new PlanRequest();
        request.setName("Updated Plan");
        request.setPrice(BigDecimal.valueOf(200));
        request.setDuration(60);

        mockMvc.perform(put("/comercial/plans/{id}", activePlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Plan"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void update_shouldReturn409_whenNameDuplicated() throws Exception {
        var request = new PlanRequest();
        request.setName("Silver Plan");
        request.setPrice(BigDecimal.valueOf(200));
        request.setDuration(30);

        mockMvc.perform(put("/comercial/plans/{id}", activePlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void inactivate_shouldReturn204() throws Exception {
        mockMvc.perform(post("/comercial/plans/{id}/inactivate", activePlan.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void inactivate_shouldReturn409_whenAlreadyInactive() throws Exception {
        mockMvc.perform(post("/comercial/plans/{id}/inactivate", inactivePlan.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void activate_shouldReturn204() throws Exception {
        mockMvc.perform(post("/comercial/plans/{id}/activate", inactivePlan.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void activate_shouldReturn409_whenAlreadyActive() throws Exception {
        mockMvc.perform(post("/comercial/plans/{id}/activate", activePlan.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void readEndpoints_shouldBeAccessibleByReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);

        mockMvc.perform(get("/comercial/plans"))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpoints_shouldBeForbiddenForReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);

        var request = new PlanRequest();
        request.setName("Forbidden Plan");
        request.setPrice(BigDecimal.TEN);
        request.setDuration(30);

        mockMvc.perform(post("/comercial/plans")
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

    private Plan createPlan(Studio studio, String name, BigDecimal price, Integer duration, boolean active) {
        var plan = new Plan();
        plan.setStudio(studio);
        plan.setName(name);
        plan.setPrice(price);
        plan.setDuration(duration);
        plan.setVersion(1);
        plan.setActive(active);
        return plan;
    }
}
