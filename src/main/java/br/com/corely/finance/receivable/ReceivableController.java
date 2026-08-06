package br.com.corely.finance.receivable;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.finance.dto.DueDateRequest;
import br.com.corely.finance.movement.ReceivableMovementService;
import br.com.corely.finance.movement.dto.MovementResponse;
import br.com.corely.finance.receivable.dto.ReceivableRequest;
import br.com.corely.finance.receivable.dto.ReceivableResponse;
import br.com.corely.finance.situation.Situation;
import br.com.corely.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Endpoints de contas a receber — recebíveis (EPIC-03-S01/S03).
 */
@RestController("receivableController")
@RequestMapping("/finance/receivables")
@RequiredArgsConstructor
@Tag(name = "Financeiro", description = "Contas a receber — recebíveis")
public class ReceivableController {

    private final ReceivableService receivableService;
    private final ReceivableMovementService movementService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    @Operation(summary = "Criar recebível", description = "Cria um título a receber de um aluno do estúdio corrente.")
    public ResponseEntity<ReceivableResponse> create(@Valid @RequestBody ReceivableRequest request) {
        var response = receivableService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Listar recebíveis",
            description = "Lista recebíveis do estúdio corrente com filtros opcionais por situação (em aberto, paga, vencida, estornada), aluno e vencimento (paginado).")
    public ResponseEntity<Page<ReceivableResponse>> findAll(
            @Parameter(description = "Filtro por situação financeira") @RequestParam(required = false) Situation situation,
            @Parameter(description = "Filtro por status persistido (backward compatible)") @RequestParam(required = false) ReceivableStatus status,
            @Parameter(description = "Filtro por aluno") @RequestParam(required = false) UUID studentId,
            @Parameter(description = "Vencimento inicial (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @Parameter(description = "Vencimento final (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            Pageable pageable) {
        if (situation != null) {
            return ResponseEntity.ok(
                    receivableService.findBySituation(situation, studentId, dueDateFrom, dueDateTo, pageable));
        }
        return ResponseEntity.ok(receivableService.findAll(status, studentId, dueDateFrom, dueDateTo, pageable));
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Buscar recebível por ID")
    public ResponseEntity<ReceivableResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(receivableService.findById(id));
    }

    @PatchMapping("/{id}/due-date")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    @Operation(summary = "Reagendar vencimento de recebível",
            description = "Atualiza a data de vencimento de um recebível em aberto (não é permitido em recebíveis pagos ou estornados).")
    public ResponseEntity<ReceivableResponse> updateDueDate(@PathVariable UUID id,
                                                            @Valid @RequestBody DueDateRequest request) {
        return ResponseEntity.ok(receivableService.updateDueDate(id, request.getDueDate()));
    }

    @GetMapping("/{id}/movements")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Histórico de movimentações do recebível",
            description = "Lista as movimentações (criação, pagamento, ajuste, cancelamento, mudança de vencimento) de um recebível do estúdio corrente (paginado, por data decrescente).")
    public ResponseEntity<Page<MovementResponse>> findMovements(@PathVariable UUID id, Pageable pageable) {
        return ResponseEntity.ok(movementService.findByReceivableId(id, pageable));
    }
}
