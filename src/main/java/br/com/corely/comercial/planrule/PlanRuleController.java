package br.com.corely.comercial.planrule;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.planrule.dto.PlanRuleRequest;
import br.com.corely.comercial.planrule.dto.PlanRuleResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comercial/plans/{planId}/rules")
@RequiredArgsConstructor
public class PlanRuleController {

    private final PlanRuleService planRuleService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<PlanRuleResponse> create(@PathVariable UUID planId, @Valid @RequestBody PlanRuleRequest request) {
        var response = planRuleService.create(planId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<List<PlanRuleResponse>> findAll(@PathVariable UUID planId) {
        var response = planRuleService.findAllByPlanId(planId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{ruleId}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<PlanRuleResponse> update(@PathVariable UUID planId, @PathVariable UUID ruleId, @Valid @RequestBody PlanRuleRequest request) {
        var response = planRuleService.update(planId, ruleId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{ruleId}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<Void> delete(@PathVariable UUID planId, @PathVariable UUID ruleId) {
        planRuleService.delete(planId, ruleId);
        return ResponseEntity.noContent().build();
    }
}
