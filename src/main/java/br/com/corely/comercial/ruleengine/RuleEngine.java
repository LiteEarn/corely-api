package br.com.corely.comercial.ruleengine;

import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RuleEngine {

    private final PlanRepository planRepository;
    private final PlanRuleRepository planRuleRepository;

    public RuleResult evaluate(UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        return evaluate(plan);
    }

    public RuleResult evaluate(Plan plan) {
        List<PlanRule> planRules = planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId());

        Map<String, Object> resolvedValues = new LinkedHashMap<>();

        for (PlanRule pr : planRules) {
            var def = pr.getRuleDefinition();
            resolvedValues.put(def.getCode(), RuleResolver.resolve(pr.getValue(), def.getValueType()));
        }

        return new RuleResult(resolvedValues);
    }
}
