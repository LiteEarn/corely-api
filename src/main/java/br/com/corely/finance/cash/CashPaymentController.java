package br.com.corely.finance.cash;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.finance.cash.dto.CashPaymentRequest;
import br.com.corely.finance.payment.dto.PaymentResponse;
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
 * Endpoints de pagamentos em dinheiro (EPIC-03-S09).
 */
@RestController("financeCashPaymentController")
@RequestMapping("/finance/cash/payments")
@RequiredArgsConstructor
@Tag(name = "Financeiro", description = "Pagamentos — Dinheiro")
public class CashPaymentController {

    private final CashPaymentService cashPaymentService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    @Operation(summary = "Registrar pagamento em dinheiro",
            description = "Registra a liquidação de um recebível (ou parcela) do estúdio corrente em dinheiro e atualiza a situação para paga.")
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CashPaymentRequest request) {
        var response = cashPaymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Listar pagamentos em dinheiro",
            description = "Lista os pagamentos em dinheiro do estúdio corrente (paginado, por data decrescente).")
    public ResponseEntity<Page<PaymentResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(cashPaymentService.findAll(pageable));
    }
}