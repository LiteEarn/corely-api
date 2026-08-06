package br.com.corely.finance.installment;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.finance.installment.dto.InstallmentResponse;
import br.com.corely.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Endpoints de parcelas de recebíveis (EPIC-03-S02).
 */
@RestController("receivableInstallmentController")
@RequestMapping("/finance/installments")
@RequiredArgsConstructor
@Tag(name = "Financeiro", description = "Contas a receber — parcelas")
public class ReceivableInstallmentController {

    private final ReceivableInstallmentService installmentService;

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Listar parcelas",
            description = "Lista parcelas do estúdio corrente com filtros opcionais por situação, matrícula e vencimento (paginado).")
    public ResponseEntity<Page<InstallmentResponse>> findAll(
            @Parameter(description = "Filtro por situação") @RequestParam(required = false) InstallmentStatus status,
            @Parameter(description = "Filtro por matrícula/plano") @RequestParam(required = false) UUID studentPlanId,
            @Parameter(description = "Vencimento inicial (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @Parameter(description = "Vencimento final (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            Pageable pageable) {
        return ResponseEntity.ok(installmentService.findAll(status, studentPlanId, dueDateFrom, dueDateTo, pageable));
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Buscar parcela por ID")
    public ResponseEntity<InstallmentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(installmentService.findById(id));
    }
}
