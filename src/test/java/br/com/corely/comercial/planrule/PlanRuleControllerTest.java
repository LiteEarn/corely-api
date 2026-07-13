package br.com.corely.comercial.planrule;

import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.dto.PlanRuleRequest;
import br.com.corely.comercial.ruledefinition.*;
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
class PlanRuleControllerTest {

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
    private RuleDefinitionRepository ruleDefinitionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Plan plan;
    private RuleDefinition activeRuleDef;
    private RuleDefinition inactiveRuleDef;

    @BeforeEach
    void setUp() {
        var studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        plan = planRepository.save(createPlan(studio, "Standard Plan", BigDecimal.valueOf(100), 30));
        activeRuleDef = ruleDefinitionRepository.save(createRuleDef("MAX_STUDENTS", "Maximum Students", true));
        inactiveRuleDef = ruleDefinitionRepository.save(createRuleDef("INACTIVE_RULE", "Inactive Rule", false));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());
        request.setValue("50");

        mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.planId").value(plan.getId().toString()))
                .andExpect(jsonPath("$.ruleDefinitionId").value(activeRuleDef.getId().toString()))
                .andExpect(jsonPath("$.ruleDefinitionCode").value("MAX_STUDENTS"))
                .andExpect(jsonPath("$.value").value("50"));
    }

    @Test
    void create_shouldReturn400_whenValueIsNull() throws Exception {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());

        mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400_whenValueIsBlank() throws Exception {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());
        request.setValue("   ");

        mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn409_whenRuleDefinitionInactive() throws Exception {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(inactiveRuleDef.getId());
        request.setValue("30");

        mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn409_whenDuplicate() throws Exception {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());
        request.setValue("30");
        mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturn404_whenPlanNotFound() throws Exception {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());
        request.setValue("30");

        mockMvc.perform(post("/comercial/plans/{planId}/rules", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn400_whenValidationFails() throws Exception {
        var request = new PlanRuleRequest();

        mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_shouldReturnAllRules() throws Exception {
        var anotherRule = ruleDefinitionRepository.save(createRuleDef("MIN_AGE", "Minimum Age", true));

        var request1 = new PlanRuleRequest();
        request1.setRuleDefinitionId(activeRuleDef.getId());
        request1.setValue("30");
        mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)));

        var request2 = new PlanRuleRequest();
        request2.setRuleDefinitionId(anotherRule.getId());
        request2.setValue("18");
        mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)));

        mockMvc.perform(get("/comercial/plans/{planId}/rules", plan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ruleDefinitionCode").value("MAX_STUDENTS"))
                .andExpect(jsonPath("$[1].ruleDefinitionCode").value("MIN_AGE"));
    }

    @Test
    void findAll_shouldReturnEmpty_whenNoRules() throws Exception {
        mockMvc.perform(get("/comercial/plans/{planId}/rules", plan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void findAll_shouldReturn404_whenPlanNotFound() throws Exception {
        mockMvc.perform(get("/comercial/plans/{planId}/rules", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        var createRequest = new PlanRuleRequest();
        createRequest.setRuleDefinitionId(activeRuleDef.getId());
        createRequest.setValue("30");
        var createdJson = mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var createdId = objectMapper.readTree(createdJson).get("id").asText();

        var anotherRule = ruleDefinitionRepository.save(createRuleDef("DISCOUNT", "Discount", true));
        var updateRequest = new PlanRuleRequest();
        updateRequest.setRuleDefinitionId(anotherRule.getId());
        updateRequest.setValue("10");

        mockMvc.perform(put("/comercial/plans/{planId}/rules/{ruleId}", plan.getId(), createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleDefinitionCode").value("DISCOUNT"))
                .andExpect(jsonPath("$.value").value("10"));
    }

    @Test
    void update_shouldReturn409_whenRuleDefinitionInactive() throws Exception {
        var createRequest = new PlanRuleRequest();
        createRequest.setRuleDefinitionId(activeRuleDef.getId());
        createRequest.setValue("30");
        var createdJson = mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var createdId = objectMapper.readTree(createdJson).get("id").asText();

        var updateRequest = new PlanRuleRequest();
        updateRequest.setRuleDefinitionId(inactiveRuleDef.getId());
        updateRequest.setValue("30");

        mockMvc.perform(put("/comercial/plans/{planId}/rules/{ruleId}", plan.getId(), createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void update_shouldReturn409_whenDuplicate() throws Exception {
        var anotherRule = ruleDefinitionRepository.save(createRuleDef("DISCOUNT", "Discount", true));

        var request1 = new PlanRuleRequest();
        request1.setRuleDefinitionId(activeRuleDef.getId());
        request1.setValue("30");
        mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)));

        var request2 = new PlanRuleRequest();
        request2.setRuleDefinitionId(anotherRule.getId());
        request2.setValue("10");
        var createdJson = mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var createdId = objectMapper.readTree(createdJson).get("id").asText();

        var updateRequest = new PlanRuleRequest();
        updateRequest.setRuleDefinitionId(activeRuleDef.getId());
        updateRequest.setValue("30");

        mockMvc.perform(put("/comercial/plans/{planId}/rules/{ruleId}", plan.getId(), createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void update_shouldReturn404_whenPlanRuleNotFound() throws Exception {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());
        request.setValue("30");

        mockMvc.perform(put("/comercial/plans/{planId}/rules/{ruleId}", plan.getId(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        var createRequest = new PlanRuleRequest();
        createRequest.setRuleDefinitionId(activeRuleDef.getId());
        createRequest.setValue("30");
        var createdJson = mockMvc.perform(post("/comercial/plans/{planId}/rules", plan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var createdId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/comercial/plans/{planId}/rules/{ruleId}", plan.getId(), createdId))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/comercial/plans/{planId}/rules/{ruleId}", plan.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void readEndpoints_shouldBeAccessibleByReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);

        var receptionistPlan = planRepository.save(createPlan(studio, "Receptionist Plan", BigDecimal.valueOf(50), 15));

        mockMvc.perform(get("/comercial/plans/{planId}/rules", receptionistPlan.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpoints_shouldBeForbiddenForReceptionist() throws Exception {
        var studio = studioRepository.save(createStudio("Studio Receptionist"));
        authenticateAs(studio, UserRole.RECEPTIONIST);

        var receptionistPlan = planRepository.save(createPlan(studio, "Receptionist Plan", BigDecimal.valueOf(50), 15));
        var receptionistRule = ruleDefinitionRepository.save(createRuleDef("RECEP_RULE", "Receptionist Rule", true));

        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(receptionistRule.getId());
        request.setValue("30");

        mockMvc.perform(post("/comercial/plans/{planId}/rules", receptionistPlan.getId())
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

    private Plan createPlan(Studio studio, String name, BigDecimal price, Integer duration) {
        var plan = new Plan();
        plan.setStudio(studio);
        plan.setName(name);
        plan.setPrice(price);
        plan.setDuration(duration);
        plan.setVersion(1);
        plan.setActive(true);
        return plan;
    }

    private RuleDefinition createRuleDef(String code, String name, boolean active) {
        var rule = new RuleDefinition();
        rule.setCode(code);
        rule.setName(name);
        rule.setValueType(ValueType.INTEGER);
        rule.setCategory(Category.GENERAL);
        rule.setActive(active);
        return rule;
    }
}
