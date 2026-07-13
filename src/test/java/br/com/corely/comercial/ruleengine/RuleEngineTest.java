package br.com.corely.comercial.ruleengine;

import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.Category;
import br.com.corely.comercial.ruledefinition.RuleDefinition;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private PlanRuleRepository planRuleRepository;

    private RuleEngine ruleEngine;

    private Plan plan;
    private RuleDefinition validityDays;
    private RuleDefinition autoRenew;
    private RuleDefinition billingCycle;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine(planRepository, planRuleRepository);

        plan = new Plan();
        plan.setId(UUID.randomUUID());

        validityDays = createRuleDef("VALIDITY_DAYS", ValueType.INTEGER);
        autoRenew = createRuleDef("AUTO_RENEW", ValueType.BOOLEAN);
        billingCycle = createRuleDef("BILLING_CYCLE", ValueType.STRING);
    }

    @Test
    void evaluateByPlanId_shouldReturnRuleResult() {
        when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
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
    void evaluate_shouldThrowRuleException_whenCodeNotInPlan() {
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of(
                createPlanRule(validityDays, "30")
        ));

        RuleResult result = ruleEngine.evaluate(plan);

        assertThatThrownBy(() -> result.getInteger("UNKNOWN_CODE"))
                .isInstanceOf(RuleException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void evaluate_shouldResolveStringValue() {
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of(
                createPlanRule(billingCycle, "YEARLY")
        ));

        RuleResult result = ruleEngine.evaluate(plan);

        assertThat(result.getString("BILLING_CYCLE")).isEqualTo("YEARLY");
    }

    @Test
    void evaluate_shouldResolveDecimalValue() {
        RuleDefinition pricePerClass = createRuleDef("PRICE_PER_CLASS", ValueType.DECIMAL);

        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of(
                createPlanRule(pricePerClass, "25.00")
        ));

        RuleResult result = ruleEngine.evaluate(plan);

        assertThat(result.getDecimal("PRICE_PER_CLASS")).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void evaluate_shouldReturnOnlyPlanRulesCodes() {
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of(
                createPlanRule(validityDays, "30")
        ));

        RuleResult result = ruleEngine.evaluate(plan);

        assertThat(result.getCodes()).containsExactly("VALIDITY_DAYS");
        assertThat(result.hasCode("AUTO_RENEW")).isFalse();
    }

    @Test
    void evaluate_shouldReturnEmpty_whenNoPlanRules() {
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId())).thenReturn(List.of());

        RuleResult result = ruleEngine.evaluate(plan);

        assertThat(result.getCodes()).isEmpty();
    }

    private RuleDefinition createRuleDef(String code, ValueType valueType) {
        RuleDefinition def = new RuleDefinition();
        def.setCode(code);
        def.setName(code);
        def.setValueType(valueType);
        def.setCategory(Category.GENERAL);
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
