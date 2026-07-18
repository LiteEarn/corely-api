package br.com.corely.finance.membershipplan;

import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import br.com.corely.finance.membershipplan.dto.MembershipPlanRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private Studio studioA;
    private Studio studioB;

    @BeforeEach
    void setUp() {
        studioA = studioRepository.save(createStudio("Studio A"));
        studioB = studioRepository.save(createStudio("Studio B"));

        createAndAuthenticateUser(studioA, UserRole.ADMIN, "admin-a-tenant@test.com");

        var planA = createPlan(studioA, "Plano A", BigDecimal.valueOf(199), 2);
        createPlan(studioB, "Plano B", BigDecimal.valueOf(299), 3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findAll_shouldOnlyReturnOwnTenantData() throws Exception {
        mockMvc.perform(get("/finance/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Plano A"));
    }

    @Test
    void findById_shouldReturn404_whenPlanBelongsToOtherTenant() throws Exception {
        var planB = membershipPlanRepository.findAll().stream()
                .filter(p -> p.getStudio().getId().equals(studioB.getId()))
                .findFirst().orElseThrow();

        mockMvc.perform(get("/finance/plans/{id}", planB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldCreateForAuthenticatedTenant() throws Exception {
        var request = new MembershipPlanRequest();
        request.setName("Plano Exclusivo");
        request.setDescription("Exclusivo");
        request.setMonthlyPrice(BigDecimal.valueOf(399));
        request.setSessionsPerWeek(5);

        mockMvc.perform(post("/finance/plans")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Plano Exclusivo"));
    }

    @Test
    void sameNameInDifferentStudios_shouldBeAllowed() throws Exception {
        createAndAuthenticateUser(studioB, UserRole.ADMIN, "admin-b-tenant-" + UUID.randomUUID() + "@test.com");

        var request = new MembershipPlanRequest();
        request.setName("Plano A");
        request.setMonthlyPrice(BigDecimal.valueOf(199));
        request.setSessionsPerWeek(2);

        mockMvc.perform(post("/finance/plans")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private MembershipPlan createPlan(Studio studio, String name, BigDecimal price, int sessionsPerWeek) {
        var plan = new MembershipPlan();
        plan.setStudio(studio);
        plan.setName(name);
        plan.setMonthlyPrice(price);
        plan.setSessionsPerWeek(sessionsPerWeek);
        plan.setActive(true);
        return membershipPlanRepository.save(plan);
    }

    private void createAndAuthenticateUser(Studio studio, UserRole role, String email) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(email);
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
