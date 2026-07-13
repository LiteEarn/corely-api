package br.com.corely.comercial.ruleengine;

import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.RuleDefinition;
import br.com.corely.comercial.ruledefinition.RuleDefinitionRepository;
import br.com.corely.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RuleEngine {

    private final PlanRepository planRepository;
    private final PlanRuleRepository planRuleRepository;
    private final RuleDefinitionRepository ruleDefinitionRepository;

    public RuleResult evaluate(UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        return evaluate(plan);
    }

    public RuleResult evaluate(Plan plan) {
        List<RuleDefinition> allDefs = ruleDefinitionRepository.findByActiveTrueOrderByName();
        List<PlanRule> planRules = planRuleRepository.findByPlanIdOrderByCreatedAt(plan.getId());

        Map<String, PlanRule> planRuleByCode = new HashMap<>();
        for (PlanRule pr : planRules) {
            planRuleByCode.put(pr.getRuleDefinition().getCode(), pr);
        }

        Map<String, Object> resolvedValues = new LinkedHashMap<>();
        Map<String, RuleDefinition> defsByCode = new LinkedHashMap<>();
        Set<String> missingRequired = new LinkedHashSet<>();

        for (RuleDefinition def : allDefs) {
            defsByCode.put(def.getCode(), def);
            PlanRule pr = planRuleByCode.get(def.getCode());

            if (pr != null) {
                resolvedValues.put(def.getCode(), RuleResolver.resolve(pr.getValue(), def.getValueType()));
            } else if (def.getDefaultValue() != null) {
                resolvedValues.put(def.getCode(), RuleResolver.resolve(def.getDefaultValue(), def.getValueType()));
            } else if (def.getRequired()) {
                missingRequired.add(def.getCode());
            }
        }

        return new RuleResult(resolvedValues, defsByCode, missingRequired);
    }
}
