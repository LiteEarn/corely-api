package br.com.corely.finance.refund;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.finance.refund.dto.RefundRequest;
import br.com.corely.finance.refund.dto.RefundResponse;
import br.com.corely.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de estorno de pagamentos (EPIC-03-S10).
 */
@RestController("financeRefundController")
@RequestMapping("/finance/refunds")
@RequiredArgsConstructor
@Tag(name = "Financeiro", description = "Pagamentos — Estorno")
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    @Operation(summary = "Estornar pagamento",
            description = "Estorna um pagamento do estúdio corrente, devolvendo o recebível (ou parcela) à situação de aberto e registrando a movimentação no histórico.")
    public ResponseEntity<RefundResponse> create(@Valid @RequestBody RefundRequest request) {
        var response = refundService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Listar estornos",
            description = "Lista os pagamentos estornados do estúdio corrente (paginado, por data de estorno decrescente).")
    public ResponseEntity<Page<RefundResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(refundService.findAll(pageable));
    }
}
