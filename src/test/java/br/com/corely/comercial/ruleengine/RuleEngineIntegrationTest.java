package br.com.corely.comercial.ruleengine;

import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.*;
import br.com.corely.comercial.tenant.ComercialTenantContext;
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

    @Autowired
    private ComercialTenantContext tenantContext;

    private Studio studio;
    private Plan plan;
    private RuleDefinition validityDays;
    private RuleDefinition autoRenew;
    private RuleDefinition billingCycle;
    private RuleDefinition maxClasses;
    private RuleDefinition allowMakeup;
    private RuleDefinition activeOnPayment;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Rule Engine Studio"));
        authenticateAs(studio, UserRole.OWNER);

        plan = planRepository.save(createPlan("Engine Plan", BigDecimal.valueOf(99), 30));

        validityDays = ruleDefinitionRepository.save(createRuleDef("VALIDITY_DAYS", ValueType.INTEGER, Category.VALIDITY, true, "30"));
        autoRenew = ruleDefinitionRepository.save(createRuleDef("AUTO_RENEW", ValueType.BOOLEAN, Category.BILLING, true, "true"));
        billingCycle = ruleDefinitionRepository.save(createRuleDef("BILLING_CYCLE", ValueType.STRING, Category.BILLING, true, "MONTHLY"));
        maxClasses = ruleDefinitionRepository.save(createRuleDef("MAX_CLASSES", ValueType.INTEGER, Category.ATTENDANCE, true, "0"));
        allowMakeup = ruleDefinitionRepository.save(createRuleDef("ALLOW_MAKEUP", ValueType.BOOLEAN, Category.CANCELLATION, false, null));
        activeOnPayment = ruleDefinitionRepository.save(createRuleDef("ACTIVE_ON_PAYMENT", ValueType.BOOLEAN, Category.GENERAL, true, "true"));
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
    void evaluate_shouldUseDefaultValue_whenNotConfigured() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getInteger("VALIDITY_DAYS")).isEqualTo(30);
        assertThat(result.getBoolean("AUTO_RENEW")).isTrue();
        assertThat(result.getString("BILLING_CYCLE")).isEqualTo("MONTHLY");
        assertThat(result.getInteger("MAX_CLASSES")).isZero();
    }

    @Test
    void evaluate_shouldThrowRuleException_whenRequiredRuleMissing() {
        RuleDefinition gracePeriod = ruleDefinitionRepository.save(
                createRuleDef("GRACE_PERIOD_DAYS", ValueType.INTEGER, Category.BILLING, true, null));

        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThatThrownBy(() -> result.getInteger("GRACE_PERIOD_DAYS"))
                .isInstanceOf(RuleException.class)
                .hasMessageContaining("Required rule");
    }

    @Test
    void evaluate_shouldReturnNull_whenOptionalRuleNotConfigured() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getBoolean("ALLOW_MAKEUP")).isNull();
    }

    @Test
    void evaluate_shouldThrowRuleException_whenCodeNotFound() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThatThrownBy(() -> result.getInteger("NONEXISTENT"))
                .isInstanceOf(RuleException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void evaluate_shouldThrowResourceNotFoundException_whenPlanNotFound() {
        assertThatThrownBy(() -> ruleEngine.evaluate(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found");
    }

    @Test
    void evaluate_shouldResolveStringValue() {
        planRuleRepository.save(createPlanRule(plan, billingCycle, "YEARLY"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getString("BILLING_CYCLE")).isEqualTo("YEARLY");
    }

    @Test
    void evaluate_shouldResolveDecimalValue() {
        RuleDefinition pricePerClass = ruleDefinitionRepository.save(
                createRuleDef("PRICE_PER_CLASS", ValueType.DECIMAL, Category.BILLING, false, "15.50"));

        planRuleRepository.save(createPlanRule(plan, pricePerClass, "25.00"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getDecimal("PRICE_PER_CLASS")).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void evaluate_shouldUsePlanRuleValueOverDefault() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "60"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getInteger("VALIDITY_DAYS")).isEqualTo(60);
    }

    @Test
    void evaluate_shouldReturnAllCodes() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getCodes()).contains(
                "VALIDITY_DAYS", "AUTO_RENEW", "BILLING_CYCLE", "MAX_CLASSES", "ACTIVE_ON_PAYMENT");
    }

    @Test
    void evaluate_shouldBeTenantIsolated() {
        Studio otherStudio = studioRepository.save(createStudio("Other Studio"));
        Plan otherPlan = planRepository.save(createPlan("Other Plan", BigDecimal.valueOf(50), 15));
        planRuleRepository.save(createPlanRule(otherPlan, validityDays, "999"));

        RuleResult result = ruleEngine.evaluate(plan.getId());
        assertThat(result.getInteger("VALIDITY_DAYS")).isEqualTo(30);
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

    private RuleDefinition createRuleDef(String code, ValueType valueType, Category category, boolean required, String defaultValue) {
        var rule = new RuleDefinition();
        rule.setCode(code);
        rule.setName(code);
        rule.setValueType(valueType);
        rule.setCategory(category);
        rule.setRequired(required);
        rule.setDefaultValue(defaultValue);
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
