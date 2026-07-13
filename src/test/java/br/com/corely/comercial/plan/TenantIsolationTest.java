package br.com.corely.comercial.plan;

import br.com.corely.comercial.plan.dto.PlanRequest;
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
    private PlanRepository planRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private Studio studioA;
    private Studio studioB;
    private Plan planA;
    private Plan planB;

    @BeforeEach
    void setUp() {
        studioA = studioRepository.save(createStudio("Studio A"));
        studioB = studioRepository.save(createStudio("Studio B"));

        var user = createAndAuthenticateUser(studioA, UserRole.ADMIN);

        planA = planRepository.save(createPlan(studioA, "Plan A"));
        planB = planRepository.save(createPlan(studioB, "Plan B"));

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listPlans_shouldOnlyReturnPlansFromCurrentTenant() throws Exception {
        mockMvc.perform(get("/comercial/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Plan A"));
    }

    @Test
    void findPlanById_shouldReturn404_whenPlanBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/comercial/plans/{id}", planB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findPlanById_shouldReturn200_whenPlanBelongsToCurrentTenant() throws Exception {
        mockMvc.perform(get("/comercial/plans/{id}", planA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plan A"));
    }

    @Test
    void updatePlan_shouldReturn404_whenPlanBelongsToOtherTenant() throws Exception {
        var request = new PlanRequest();
        request.setName("Hacked Plan");
        request.setPrice(BigDecimal.TEN);
        request.setDuration(30);

        mockMvc.perform(put("/comercial/plans/{id}", planB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePlan_shouldReturn404_whenPlanBelongsToOtherTenant() throws Exception {
        mockMvc.perform(delete("/comercial/plans/{id}", planB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void inactivatePlan_shouldReturn404_whenPlanBelongsToOtherTenant() throws Exception {
        mockMvc.perform(post("/comercial/plans/{id}/inactivate", planB.getId()))
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

    private Plan createPlan(Studio studio, String name) {
        var plan = new Plan();
        plan.setStudio(studio);
        plan.setName(name);
        plan.setPrice(BigDecimal.valueOf(100));
        plan.setDuration(30);
        plan.setVersion(1);
        plan.setActive(true);
        return plan;
    }
}
