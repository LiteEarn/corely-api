package br.com.corely.comercial.contractsnapshot;

import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.ruleengine.RuleEngine;
import br.com.corely.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractSnapshotService {

    private final ContractSnapshotRepository repository;
    private final PlanRepository planRepository;
    private final RuleEngine ruleEngine;
    private final ObjectMapper objectMapper;

    @Transactional
    public ContractSnapshot create(UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        var ruleResult = ruleEngine.evaluate(plan);

        String rulesJson;
        try {
            rulesJson = objectMapper.writeValueAsString(ruleResult.toMap());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize plan rules to JSON", e);
        }

        ContractSnapshot snapshot = new ContractSnapshot();
        snapshot.setStudioId(plan.getStudio().getId());
        snapshot.setPlanId(plan.getId());
        snapshot.setPlanVersion(plan.getVersion());
        snapshot.setPlanName(plan.getName());
        snapshot.setPlanDescription(plan.getDescription());
        snapshot.setPlanPrice(plan.getPrice());
        snapshot.setPlanDuration(plan.getDuration());
        snapshot.setRules(rulesJson);

        return repository.save(snapshot);
    }
}
