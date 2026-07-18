package br.com.corely.finance.membershipplan;

import br.com.corely.finance.membershipplan.dto.MembershipPlanRequest;
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
class MembershipPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private MembershipPlan existingPlan;
    private String adminToken;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Plan Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        existingPlan = new MembershipPlan();
        existingPlan.setStudio(studio);
        existingPlan.setName("Plano Básico");
        existingPlan.setDescription("Plano básico");
        existingPlan.setMonthlyPrice(BigDecimal.valueOf(199));
        existingPlan.setSessionsPerWeek(2);
        existingPlan.setActive(true);
        existingPlan = membershipPlanRepository.save(existingPlan);
    }

    @Test
    void create_shouldReturn201() throws Exception {
        var request = new MembershipPlanRequest();
        request.setName("Plano Premium");
        request.setDescription("Plano premium");
        request.setMonthlyPrice(BigDecimal.valueOf(299));
        request.setSessionsPerWeek(3);

        mockMvc.perform(post("/finance/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Plano Premium"))
                .andExpect(jsonPath("$.monthlyPrice").value(299))
                .andExpect(jsonPath("$.sessionsPerWeek").value(3));
    }

    @Test
    void create_shouldReturn409_whenDuplicateName() throws Exception {
        var request = new MembershipPlanRequest();
        request.setName("Plano Básico");
        request.setDescription("Duplicado");
        request.setMonthlyPrice(BigDecimal.valueOf(199));
        request.setSessionsPerWeek(2);

        mockMvc.perform(post("/finance/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void findAll_shouldReturnList() throws Exception {
        mockMvc.perform(get("/finance/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Plano Básico"));
    }

    @Test
    void findById_shouldReturnPlan() throws Exception {
        mockMvc.perform(get("/finance/plans/{id}", existingPlan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plano Básico"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/finance/plans/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturnUpdatedPlan() throws Exception {
        var request = new MembershipPlanRequest();
        request.setName("Plano Atualizado");
        request.setDescription("Atualizado");
        request.setMonthlyPrice(BigDecimal.valueOf(249));
        request.setSessionsPerWeek(4);

        mockMvc.perform(put("/finance/plans/{id}", existingPlan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plano Atualizado"))
                .andExpect(jsonPath("$.monthlyPrice").value(249))
                .andExpect(jsonPath("$.sessionsPerWeek").value(4));
    }

    @Test
    void delete_shouldDeactivatePlan() throws Exception {
        mockMvc.perform(delete("/finance/plans/{id}", existingPlan.getId()))
                .andExpect(status().isNoContent());

        var deactivated = membershipPlanRepository.findById(existingPlan.getId()).orElseThrow();
        assert !deactivated.getActive() : "Plan should be inactive";
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/finance/plans/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void receptionist_shouldOnlyRead() throws Exception {
        authenticateAs(studio, UserRole.RECEPTIONIST);

        mockMvc.perform(get("/finance/plans"))
                .andExpect(status().isOk());

        var request = new MembershipPlanRequest();
        request.setName("Plano Teste");
        request.setMonthlyPrice(BigDecimal.valueOf(100));
        request.setSessionsPerWeek(1);

        mockMvc.perform(post("/finance/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void financial_shouldHaveFullAccess() throws Exception {
        authenticateAs(studio, UserRole.FINANCIAL);

        var request = new MembershipPlanRequest();
        request.setName("Plano Financial");
        request.setDescription("Criado pelo financeiro");
        request.setMonthlyPrice(BigDecimal.valueOf(199));
        request.setSessionsPerWeek(2);

        mockMvc.perform(post("/finance/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void sameNameInDifferentStudios_shouldBeAllowed() throws Exception {
        var studio2 = studioRepository.save(createStudio("Second Studio"));

        authenticateAs(studio2, UserRole.ADMIN);

        var request = new MembershipPlanRequest();
        request.setName("Plano Básico");
        request.setMonthlyPrice(BigDecimal.valueOf(199));
        request.setSessionsPerWeek(2);

        mockMvc.perform(post("/finance/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "_" + UUID.randomUUID() + "@test.com");
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
}
