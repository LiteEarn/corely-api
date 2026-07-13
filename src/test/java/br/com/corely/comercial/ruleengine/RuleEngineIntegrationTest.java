package br.com.corely.comercial.ruleengine;

import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.*;
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
class RuleEngineIntegrationTest {

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanRuleRepository planRuleRepository;

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
    private RuleDefinition validityDays;
    private RuleDefinition autoRenew;
    private RuleDefinition billingCycle;
    private RuleDefinition pricePerClass;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Rule Engine Studio"));
        authenticateAs(studio, UserRole.OWNER);

        plan = planRepository.save(createPlan("Engine Plan", BigDecimal.valueOf(99), 30));

        validityDays = ruleDefinitionRepository.save(createRuleDef("VALIDITY_DAYS", ValueType.INTEGER));
        autoRenew = ruleDefinitionRepository.save(createRuleDef("AUTO_RENEW", ValueType.BOOLEAN));
        billingCycle = ruleDefinitionRepository.save(createRuleDef("BILLING_CYCLE", ValueType.STRING));
        pricePerClass = ruleDefinitionRepository.save(createRuleDef("PRICE_PER_CLASS", ValueType.DECIMAL));
    }

    @Test
    void evaluate_shouldResolveTypedValues() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "45"));
        planRuleRepository.save(createPlanRule(plan, autoRenew, "false"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getInteger("VALIDITY_DAYS")).isEqualTo(45);
        assertThat(result.getBoolean("AUTO_RENEW")).isFalse();
    }

    @Test
    void evaluate_shouldResolveStringValue() {
        planRuleRepository.save(createPlanRule(plan, billingCycle, "YEARLY"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getString("BILLING_CYCLE")).isEqualTo("YEARLY");
    }

    @Test
    void evaluate_shouldResolveDecimalValue() {
        planRuleRepository.save(createPlanRule(plan, pricePerClass, "25.00"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getDecimal("PRICE_PER_CLASS")).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void evaluate_shouldThrowRuleException_whenCodeNotInPlan() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThatThrownBy(() -> result.getInteger("NONEXISTENT"))
                .isInstanceOf(RuleException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void evaluate_shouldReturnOnlyPlanRulesCodes() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getCodes()).containsExactly("VALIDITY_DAYS");
        assertThat(result.hasCode("AUTO_RENEW")).isFalse();
    }

    @Test
    void evaluate_shouldReturnEmpty_whenNoPlanRules() {
        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getCodes()).isEmpty();
    }

    @Test
    void evaluate_shouldThrowResourceNotFoundException_whenPlanNotFound() {
        assertThatThrownBy(() -> ruleEngine.evaluate(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found");
    }

    @Test
    void evaluate_shouldResolveMultiplePlanRules() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));
        planRuleRepository.save(createPlanRule(plan, autoRenew, "true"));
        planRuleRepository.save(createPlanRule(plan, billingCycle, "MONTHLY"));
        planRuleRepository.save(createPlanRule(plan, pricePerClass, "15.50"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getInteger("VALIDITY_DAYS")).isEqualTo(30);
        assertThat(result.getBoolean("AUTO_RENEW")).isTrue();
        assertThat(result.getString("BILLING_CYCLE")).isEqualTo("MONTHLY");
        assertThat(result.getDecimal("PRICE_PER_CLASS")).isEqualByComparingTo(new BigDecimal("15.50"));
        assertThat(result.getCodes()).hasSize(4);
    }

    @Test
    void evaluate_shouldNotIncludeRulesFromOtherPlans() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        Plan otherPlan = planRepository.save(createPlan("Other Plan", BigDecimal.valueOf(50), 15));
        planRuleRepository.save(createPlanRule(otherPlan, autoRenew, "true"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getCodes()).containsExactly("VALIDITY_DAYS");
    }

    @Test
    void evaluate_shouldNotReturnRulesFromOtherPlans() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getCodes()).containsExactly("VALIDITY_DAYS");
        assertThatThrownBy(() -> result.getString("BILLING_CYCLE"))
                .isInstanceOf(RuleException.class);
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

    private RuleDefinition createRuleDef(String code, ValueType valueType) {
        var rule = new RuleDefinition();
        rule.setCode(code);
        rule.setName(code);
        rule.setValueType(valueType);
        rule.setCategory(Category.GENERAL);
        rule.setActive(true);
        return rule;
    }

    private PlanRule createPlanRule(Plan plan, RuleDefinition ruleDef, String value) {
        var pr = new PlanRule();
        pr.setStudio(studio);
        pr.setPlan(plan);
        pr.setRuleDefinition(ruleDef);
        pr.setValue(value);
        return pr;
    }
}
