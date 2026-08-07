package br.com.corely.finance.card;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.finance.card.dto.CardPaymentRequest;
import br.com.corely.finance.card.dto.CardPaymentResponse;
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
 * Endpoints de transações de cartão (EPIC-03-S08).
 */
@RestController("financeCardPaymentController")
@RequestMapping("/finance/card/payments")
@RequiredArgsConstructor
@Tag(name = "Financeiro", description = "Pagamentos — Cartão")
public class CardPaymentController {

    private final CardPaymentService cardPaymentService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    @Operation(summary = "Gerar transação de cartão",
            description = "Gera uma transação de cartão para um recebível em aberto do estúdio corrente, retornando o identificador da transação.")
    public ResponseEntity<CardPaymentResponse> create(@Valid @RequestBody CardPaymentRequest request) {
        var response = cardPaymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{transactionId}/confirm")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    @Operation(summary = "Conciliar transação de cartão",
            description = "Confirma o pagamento via cartão pelo identificador da transação, registra a baixa (método CREDIT_CARD), liquida o recebível e registra a movimentação no histórico.")
    public ResponseEntity<CardPaymentResponse> confirm(@PathVariable String transactionId) {
        return ResponseEntity.ok(cardPaymentService.confirm(transactionId));
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Listar transações de cartão",
            description = "Lista as transações de cartão do estúdio corrente (paginado, por data de criação decrescente).")
    public ResponseEntity<Page<CardPaymentResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(cardPaymentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Buscar transação de cartão por ID")
    public ResponseEntity<CardPaymentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(cardPaymentService.findById(id));
    }
}