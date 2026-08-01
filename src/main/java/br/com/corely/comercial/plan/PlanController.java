package br.com.corely.comercial.plan;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.plan.dto.PlanRequest;
import br.com.corely.comercial.plan.dto.PlanResponse;
import br.com.corely.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/comercial/plans")
@RequiredArgsConstructor
@Tag(name = "Planos Comerciais", description = "Gerenciamento de planos comerciais do estudio")
public class PlanController {

    private final PlanService planService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    @Operation(summary = "Criar plano comercial", description = "Cria um novo plano comercial. Nome deve ser unico por estudio.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plano criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos ou nome duplicado")
    })
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody PlanRequest request) {
        var response = planService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    @Operation(summary = "Listar planos comerciais", description = "Lista planos com filtros opcionais por nome e status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    })
    public ResponseEntity<Page<PlanResponse>> findAll(
            @Parameter(description = "Filtro por nome (parcial, case-insensitive)") @RequestParam(required = false) String name,
            @Parameter(description = "Filtro por status ativo/inativo") @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        var response = planService.findAll(name, active, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    @Operation(summary = "Buscar plano por ID", description = "Retorna os dados completos do plano incluindo contagem de alunos ativos e regras")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano encontrado"),
            @ApiResponse(responseCode = "404", description = "Plano nao encontrado")
    })
    public ResponseEntity<PlanResponse> findById(@PathVariable UUID id) {
        var response = planService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    @Operation(summary = "Atualizar plano comercial", description = "Atualiza dados do plano. A versao e incrementada automaticamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos ou nome duplicado"),
            @ApiResponse(responseCode = "404", description = "Plano nao encontrado")
    })
    public ResponseEntity<PlanResponse> update(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        var response = planService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    @Operation(summary = "Ativar plano", description = "Ativa um plano inativo")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Plano ativado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Plano ja esta ativo"),
            @ApiResponse(responseCode = "404", description = "Plano nao encontrado")
    })
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        planService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/inactivate")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    @Operation(summary = "Inativar plano", description = "Inativa um plano. Nao e permitido inativar planos com contratos ativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Plano inativado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Plano ja inativo ou possui contratos ativos"),
            @ApiResponse(responseCode = "404", description = "Plano nao encontrado")
    })
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        planService.inactivate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    @Operation(summary = "Excluir plano comercial",
               description = "Exclui um plano. Nao e permitido excluir planos com contratos ativos ou suspensos. Regras associadas sao removidas automaticamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Plano excluido com sucesso"),
            @ApiResponse(responseCode = "400", description = "Plano possui contratos ativos ou suspensos"),
            @ApiResponse(responseCode = "404", description = "Plano nao encontrado")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        planService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
