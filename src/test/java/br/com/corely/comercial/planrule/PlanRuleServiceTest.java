package br.com.corely.comercial.planrule;

import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.dto.PlanRuleRequest;
import br.com.corely.comercial.ruledefinition.*;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlanRuleServiceTest {

    @Autowired
    private PlanRuleService planRuleService;

    @Autowired
    private PlanRuleRepository planRuleRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private RuleDefinitionRepository ruleDefinitionRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Plan plan;
    private RuleDefinition activeRuleDef;
    private RuleDefinition inactiveRuleDef;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        plan = planRepository.save(createPlan("Standard Plan", BigDecimal.valueOf(100), 30));

        activeRuleDef = ruleDefinitionRepository.save(createRuleDef("MAX_STUDENTS", "Maximum Students", true));
        inactiveRuleDef = ruleDefinitionRepository.save(createRuleDef("INACTIVE_RULE", "Inactive Rule", false));
    }

    @Test
    void create_shouldPersistPlanRule() {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());
        request.setValue("50");

        var response = planRuleService.create(plan.getId(), request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getPlanId()).isEqualTo(plan.getId());
        assertThat(response.getRuleDefinitionId()).isEqualTo(activeRuleDef.getId());
        assertThat(response.getRuleDefinitionCode()).isEqualTo("MAX_STUDENTS");
        assertThat(response.getValue()).isEqualTo("50");
    }

    @Test
    void create_shouldThrowException_whenRuleDefinitionInactive() {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(inactiveRuleDef.getId());

        assertThatThrownBy(() -> planRuleService.create(plan.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive RuleDefinition");
    }

    @Test
    void create_shouldThrowException_whenDuplicateAssociation() {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());
        planRuleService.create(plan.getId(), request);

        var duplicate = new PlanRuleRequest();
        duplicate.setRuleDefinitionId(activeRuleDef.getId());

        assertThatThrownBy(() -> planRuleService.create(plan.getId(), duplicate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already associated");
    }

    @Test
    void create_shouldThrowException_whenPlanNotFound() {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());

        assertThatThrownBy(() -> planRuleService.create(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found");
    }

    @Test
    void create_shouldThrowException_whenRuleDefinitionNotFound() {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(UUID.randomUUID());

        assertThatThrownBy(() -> planRuleService.create(plan.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("RuleDefinition not found");
    }

    @Test
    void findAllByPlanId_shouldReturnAllRules() {
        var otherRule = ruleDefinitionRepository.save(createRuleDef("MIN_AGE", "Minimum Age", true));

        var request1 = new PlanRuleRequest();
        request1.setRuleDefinitionId(activeRuleDef.getId());
        request1.setValue("30");
        planRuleService.create(plan.getId(), request1);

        var request2 = new PlanRuleRequest();
        request2.setRuleDefinitionId(otherRule.getId());
        request2.setValue("18");
        planRuleService.create(plan.getId(), request2);

        var rules = planRuleService.findAllByPlanId(plan.getId());

        assertThat(rules).hasSize(2);
        assertThat(rules.get(0).getRuleDefinitionCode()).isEqualTo("MAX_STUDENTS");
        assertThat(rules.get(1).getRuleDefinitionCode()).isEqualTo("MIN_AGE");
    }

    @Test
    void findAllByPlanId_shouldReturnEmpty_whenNoRules() {
        var rules = planRuleService.findAllByPlanId(plan.getId());

        assertThat(rules).isEmpty();
    }

    @Test
    void findAllByPlanId_shouldThrowException_whenPlanNotFound() {
        assertThatThrownBy(() -> planRuleService.findAllByPlanId(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found");
    }

    @Test
    void update_shouldModifyPlanRule() {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());
        request.setValue("30");
        var created = planRuleService.create(plan.getId(), request);

        var anotherRule = ruleDefinitionRepository.save(createRuleDef("DISCOUNT", "Discount", true));
        var updateRequest = new PlanRuleRequest();
        updateRequest.setRuleDefinitionId(anotherRule.getId());
        updateRequest.setValue("10");

        var response = planRuleService.update(plan.getId(), created.getId(), updateRequest);

        assertThat(response.getRuleDefinitionId()).isEqualTo(anotherRule.getId());
        assertThat(response.getRuleDefinitionCode()).isEqualTo("DISCOUNT");
        assertThat(response.getValue()).isEqualTo("10");
    }

    @Test
    void update_shouldThrowException_whenRuleDefinitionInactive() {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());
        var created = planRuleService.create(plan.getId(), request);

        var updateRequest = new PlanRuleRequest();
        updateRequest.setRuleDefinitionId(inactiveRuleDef.getId());

        assertThatThrownBy(() -> planRuleService.update(plan.getId(), created.getId(), updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive RuleDefinition");
    }

    @Test
    void update_shouldThrowException_whenDuplicateAssociation() {
        var anotherRule = ruleDefinitionRepository.save(createRuleDef("DISCOUNT", "Discount", true));

        var request1 = new PlanRuleRequest();
        request1.setRuleDefinitionId(activeRuleDef.getId());
        planRuleService.create(plan.getId(), request1);

        var request2 = new PlanRuleRequest();
        request2.setRuleDefinitionId(anotherRule.getId());
        var created2 = planRuleService.create(plan.getId(), request2);

        var updateRequest = new PlanRuleRequest();
        updateRequest.setRuleDefinitionId(activeRuleDef.getId());

        assertThatThrownBy(() -> planRuleService.update(plan.getId(), created2.getId(), updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already associated");
    }

    @Test
    void update_shouldAllowSameRuleDefinition() {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());
        request.setValue("30");
        var created = planRuleService.create(plan.getId(), request);

        var updateRequest = new PlanRuleRequest();
        updateRequest.setRuleDefinitionId(activeRuleDef.getId());
        updateRequest.setValue("50");

        var response = planRuleService.update(plan.getId(), created.getId(), updateRequest);

        assertThat(response.getValue()).isEqualTo("50");
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());

        assertThatThrownBy(() -> planRuleService.update(plan.getId(), UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("PlanRule not found");
    }

    @Test
    void delete_shouldRemovePlanRule() {
        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());
        var created = planRuleService.create(plan.getId(), request);

        planRuleService.delete(plan.getId(), created.getId());

        assertThat(planRuleRepository.findByPlanIdAndId(plan.getId(), created.getId())).isEmpty();
        assertThat(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).isEmpty();
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> planRuleService.delete(plan.getId(), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("PlanRule not found");
    }

    @Test
    void create_shouldAllowSameRuleDefinitionInDifferentPlans() {
        var otherPlan = planRepository.save(createPlan("Premium Plan", BigDecimal.valueOf(200), 60));

        var request = new PlanRuleRequest();
        request.setRuleDefinitionId(activeRuleDef.getId());
        planRuleService.create(plan.getId(), request);

        var otherRequest = new PlanRuleRequest();
        otherRequest.setRuleDefinitionId(activeRuleDef.getId());
        var otherResponse = planRuleService.create(otherPlan.getId(), otherRequest);

        assertThat(otherResponse.getId()).isNotNull();
        assertThat(otherResponse.getRuleDefinitionId()).isEqualTo(activeRuleDef.getId());
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "_" + studio.getId() + "@test.com");
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

    private Plan createPlan(String name, BigDecimal price, Integer duration) {
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
