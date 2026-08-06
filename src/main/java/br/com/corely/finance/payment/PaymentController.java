package br.com.corely.finance.payment;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.finance.payment.dto.PaymentRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints de pagamentos — baixa manual (EPIC-03-S06).
 */
@RestController("financePaymentController")
@RequestMapping("/finance/payments")
@RequiredArgsConstructor
@Tag(name = "Financeiro", description = "Pagamentos — baixa manual")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    @Operation(summary = "Registrar baixa manual de pagamento",
            description = "Registra a liquidação de um recebível (ou parcela) do estúdio corrente e atualiza a situação para paga.")
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request) {
        var response = paymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Listar pagamentos",
            description = "Lista os pagamentos (baixas manuais) do estúdio corrente (paginado, por data decrescente).")
    public ResponseEntity<Page<PaymentResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(paymentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Buscar pagamento por ID")
    public ResponseEntity<PaymentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.findById(id));
    }
}