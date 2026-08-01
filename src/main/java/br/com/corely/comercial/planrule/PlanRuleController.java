package br.com.corely.comercial.planrule;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.planrule.dto.PlanRuleRequest;
import br.com.corely.comercial.planrule.dto.PlanRuleResponse;
import br.com.corely.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Regras de Plano", description = "Gerenciamento de regras associadas a planos comerciais")
public class PlanRuleController {

    private final PlanRuleService planRuleService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    @Operation(summary = "Associar regra a um plano", description = "Associa uma definicao de regra a um plano com um valor especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Regra associada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Regra ja associada ou definicao inativa"),
            @ApiResponse(responseCode = "404", description = "Plano ou RuleDefinition nao encontrado")
    })
    public ResponseEntity<PlanRuleResponse> create(
            @Parameter(description = "ID do plano") @PathVariable UUID planId,
            @Valid @RequestBody PlanRuleRequest request) {
        var response = planRuleService.create(planId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    @Operation(summary = "Listar regras do plano", description = "Lista todas as regras associadas a um plano")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Plano nao encontrado")
    })
    public ResponseEntity<List<PlanRuleResponse>> findAll(
            @Parameter(description = "ID do plano") @PathVariable UUID planId) {
        var response = planRuleService.findAllByPlanId(planId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{ruleId}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    @Operation(summary = "Atualizar regra do plano", description = "Atualiza o valor ou a definicao de uma regra associada ao plano")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Regra atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Regra duplicada ou definicao inativa"),
            @ApiResponse(responseCode = "404", description = "Regra nao encontrada")
    })
    public ResponseEntity<PlanRuleResponse> update(
            @Parameter(description = "ID do plano") @PathVariable UUID planId,
            @Parameter(description = "ID da regra") @PathVariable UUID ruleId,
            @Valid @RequestBody PlanRuleRequest request) {
        var response = planRuleService.update(planId, ruleId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{ruleId}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    @Operation(summary = "Remover regra do plano", description = "Remove a associacao de uma regra com o plano")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Regra removida com sucesso"),
            @ApiResponse(responseCode = "404", description = "Regra nao encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do plano") @PathVariable UUID planId,
            @Parameter(description = "ID da regra") @PathVariable UUID ruleId) {
        planRuleService.delete(planId, ruleId);
        return ResponseEntity.noContent().build();
    }
}
