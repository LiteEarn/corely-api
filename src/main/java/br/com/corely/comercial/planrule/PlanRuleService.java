package br.com.corely.comercial.planrule;

import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.dto.PlanRuleRequest;
import br.com.corely.comercial.planrule.dto.PlanRuleResponse;
import br.com.corely.comercial.ruledefinition.RuleDefinitionRepository;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanRuleService {

    private final PlanRuleRepository planRuleRepository;
    private final PlanRepository planRepository;
    private final RuleDefinitionRepository ruleDefinitionRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public PlanRuleResponse create(UUID planId, PlanRuleRequest request) {
        var plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        var ruleDef = ruleDefinitionRepository.findById(request.getRuleDefinitionId())
                .orElseThrow(() -> new ResourceNotFoundException("RuleDefinition not found"));

        if (!ruleDef.getActive()) {
            throw new BusinessException("Cannot associate an inactive RuleDefinition");
        }

        if (planRuleRepository.existsByPlanIdAndRuleDefinitionId(planId, request.getRuleDefinitionId())) {
            throw new BusinessException("Rule already associated with this plan");
        }

        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var planRule = new PlanRule();
        planRule.setStudio(studio);
        planRule.setPlan(plan);
        planRule.setRuleDefinition(ruleDef);
        planRule.setValue(request.getValue());

        planRule = planRuleRepository.save(planRule);
        return toResponse(planRule);
    }

    @Transactional(readOnly = true)
    public List<PlanRuleResponse> findAllByPlanId(UUID planId) {
        planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        return planRuleRepository.findByPlanIdOrderByCreatedAt(planId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PlanRuleResponse update(UUID planId, UUID ruleId, PlanRuleRequest request) {
        var planRule = planRuleRepository.findByPlanIdAndId(planId, ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("PlanRule not found"));

        var ruleDef = ruleDefinitionRepository.findById(request.getRuleDefinitionId())
                .orElseThrow(() -> new ResourceNotFoundException("RuleDefinition not found"));

        if (!ruleDef.getActive()) {
            throw new BusinessException("Cannot associate an inactive RuleDefinition");
        }

        if (!planRule.getRuleDefinition().getId().equals(request.getRuleDefinitionId())) {
            if (planRuleRepository.existsByPlanIdAndRuleDefinitionId(planId, request.getRuleDefinitionId())) {
                throw new BusinessException("Rule already associated with this plan");
            }
        }

        planRule.setRuleDefinition(ruleDef);
        planRule.setValue(request.getValue());

        planRule = planRuleRepository.save(planRule);
        return toResponse(planRule);
    }

    @Transactional
    public void delete(UUID planId, UUID ruleId) {
        var planRule = planRuleRepository.findByPlanIdAndId(planId, ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("PlanRule not found"));

        planRuleRepository.delete(planRule);
    }

    private PlanRuleResponse toResponse(PlanRule planRule) {
        var rd = planRule.getRuleDefinition();
        return new PlanRuleResponse(
                planRule.getId(),
                planRule.getPlan().getId(),
                rd.getId(),
                rd.getCode(),
                rd.getName(),
                rd.getValueType(),
                planRule.getValue(),
                planRule.getCreatedAt(),
                planRule.getUpdatedAt()
        );
    }
}
