package br.com.corely.comercial.ruleengine;

import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.Category;
import br.com.corely.comercial.ruledefinition.RuleDefinition;
import br.com.corely.comercial.ruledefinition.RuleDefinitionRepository;
import br.com.corely.comercial.ruledefinition.ValueType;
import br.com.corely.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanRuleRepository planRuleRepository;

    @Mock
    private RuleDefinitionRepository ruleDefinitionRepository;

    private RuleEngine ruleEngine;

    private Plan plan;
    private RuleDefinition validityDays;
    private RuleDefinition autoRenew;
    private RuleDefinition billingCycle;
    private RuleDefinition maxClasses;
    private RuleDefinition allowMakeup;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine(planRepository, planRuleRepository, ruleDefinitionRepository);

        plan = new Plan();
        plan.setId(UUID.randomUUID());

        validityDays = createRuleDef("VALIDITY_DAYS", ValueType.INTEGER, Category.VALIDITY, true, "30");
        autoRenew = createRuleDef("AUTO_RENEW", ValueType.BOOLEAN, Category.BILLING, true, "true");
        billingCycle = createRuleDef("BILLING_CYCLE", ValueType.STRING, Category.BILLING, true, "MONTHLY");
        maxClasses = createRuleDef("MAX_CLASSES", ValueType.INTEGER, Category.ATTENDANCE, true, "0");
        allowMakeup = createRuleDef("ALLOW_MAKEUP", ValueType.BOOLEAN, Category.CANCELLATION, false, null);
    }

    @Test
    void evaluateByPlanId_shouldReturnRuleResult() {
        when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
        when(ruleDefinitionRepository.findByActiveTrueOrderByName()).thenReturn(List.of(validityDays, autoRenew));
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of(
                createPlanRule(validityDays, "45"),
                createPlanRule(autoRenew, "false")
        ));

        RuleResult result = ruleEngine.evaluate(plan.getId());

        assertThat(result.getInteger("VALIDITY_DAYS")).isEqualTo(45);
        assertThat(result.getBoolean("AUTO_RENEW")).isFalse();
    }

    @Test
    void evaluateByPlan_shouldResolveValues() {
        when(ruleDefinitionRepository.findByActiveTrueOrderByName()).thenReturn(List.of(validityDays, autoRenew));
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of(
                createPlanRule(validityDays, "30"),
                createPlanRule(autoRenew, "true")
        ));

        RuleResult result = ruleEngine.evaluate(plan);

        assertThat(result.getInteger("VALIDITY_DAYS")).isEqualTo(30);
        assertThat(result.getBoolean("AUTO_RENEW")).isTrue();
    }

    @Test
    void evaluate_shouldThrowResourceNotFoundException_whenPlanNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(planRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleEngine.evaluate(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found");
    }

    @Test
    void evaluate_shouldUseDefaultValue_whenPlanRuleNotConfigured() {
        when(ruleDefinitionRepository.findByActiveTrueOrderByName()).thenReturn(List.of(validityDays, autoRenew));
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of());

        RuleResult result = ruleEngine.evaluate(plan);

        assertThat(result.getInteger("VALIDITY_DAYS")).isEqualTo(30);
        assertThat(result.getBoolean("AUTO_RENEW")).isTrue();
    }

    @Test
    void evaluate_shouldThrowRuleException_whenRequiredRuleMissing() {
        RuleDefinition requiredNoDefault = createRuleDef("GRACE_PERIOD_DAYS", ValueType.INTEGER, Category.BILLING, true, null);

        when(ruleDefinitionRepository.findByActiveTrueOrderByName()).thenReturn(List.of(requiredNoDefault));
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of());

        RuleResult result = ruleEngine.evaluate(plan);

        assertThatThrownBy(() -> result.getInteger("GRACE_PERIOD_DAYS"))
                .isInstanceOf(RuleException.class)
                .hasMessageContaining("Required rule")
                .hasMessageContaining("GRACE_PERIOD_DAYS");
    }

    @Test
    void evaluate_shouldReturnNull_whenOptionalRuleNotConfiguredAndNoDefault() {
        when(ruleDefinitionRepository.findByActiveTrueOrderByName()).thenReturn(List.of(allowMakeup));
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of());

        RuleResult result = ruleEngine.evaluate(plan);

        assertThat(result.getBoolean("ALLOW_MAKEUP")).isNull();
    }

    @Test
    void evaluate_shouldThrowRuleException_whenCodeNotFound() {
        when(ruleDefinitionRepository.findByActiveTrueOrderByName()).thenReturn(List.of(validityDays));
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of());

        RuleResult result = ruleEngine.evaluate(plan);

        assertThatThrownBy(() -> result.getInteger("UNKNOWN_CODE"))
                .isInstanceOf(RuleException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void evaluate_shouldResolveStringValue() {
        when(ruleDefinitionRepository.findByActiveTrueOrderByName()).thenReturn(List.of(billingCycle));
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of(
                createPlanRule(billingCycle, "YEARLY")
        ));

        RuleResult result = ruleEngine.evaluate(plan);

        assertThat(result.getString("BILLING_CYCLE")).isEqualTo("YEARLY");
    }

    @Test
    void evaluate_shouldResolveDecimalValue() {
        RuleDefinition pricePerClass = createRuleDef("PRICE_PER_CLASS", ValueType.DECIMAL, Category.BILLING, false, "15.50");

        when(ruleDefinitionRepository.findByActiveTrueOrderByName()).thenReturn(List.of(pricePerClass));
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of());

        RuleResult result = ruleEngine.evaluate(plan);

        assertThat(result.getDecimal("PRICE_PER_CLASS")).isEqualByComparingTo(new BigDecimal("15.50"));
    }

    @Test
    void evaluate_shouldReturnAllConfiguredCodes() {
        when(ruleDefinitionRepository.findByActiveTrueOrderByName()).thenReturn(List.of(validityDays, autoRenew));
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of(
                createPlanRule(validityDays, "30")
        ));

        RuleResult result = ruleEngine.evaluate(plan);

        assertThat(result.getCodes()).contains("VALIDITY_DAYS", "AUTO_RENEW");
    }

    @Test
    void evaluate_shouldReturnOnlyActiveRuleDefinitions() {
        RuleDefinition inactive = createRuleDef("INACTIVE_RULE", ValueType.STRING, Category.GENERAL, false, null);
        inactive.setActive(false);

        when(ruleDefinitionRepository.findByActiveTrueOrderByName()).thenReturn(List.of(validityDays));
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of());

        RuleResult result = ruleEngine.evaluate(plan);

        assertThat(result.getCodes()).containsExactly("VALIDITY_DAYS");
        assertThatThrownBy(() -> result.getString("INACTIVE_RULE"))
                .isInstanceOf(RuleException.class);
    }

    private RuleDefinition createRuleDef(String code, ValueType valueType, Category category, boolean required, String defaultValue) {
        RuleDefinition def = new RuleDefinition();
        def.setCode(code);
        def.setName(code);
        def.setValueType(valueType);
        def.setCategory(category);
        def.setRequired(required);
        def.setDefaultValue(defaultValue);
        def.setActive(true);
        return def;
    }

    private PlanRule createPlanRule(RuleDefinition ruleDef, String value) {
        PlanRule pr = new PlanRule();
        pr.setRuleDefinition(ruleDef);
        pr.setValue(value);
        return pr;
    }
}
