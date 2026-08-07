package br.com.corely.finance.pix;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.finance.pix.dto.PixPaymentRequest;
import br.com.corely.finance.pix.dto.PixPaymentResponse;
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
 * Endpoints de cobranças Pix (EPIC-03-S07).
 */
@RestController("financePixPaymentController")
@RequestMapping("/finance/pix/payments")
@RequiredArgsConstructor
@Tag(name = "Financeiro", description = "Pagamentos — Pix")
public class PixPaymentController {

    private final PixPaymentService pixPaymentService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    @Operation(summary = "Gerar cobrança Pix",
            description = "Gera uma cobrança Pix para um recebível em aberto do estúdio corrente, retornando o txid e o código copia-e-cola.")
    public ResponseEntity<PixPaymentResponse> create(@Valid @RequestBody PixPaymentRequest request) {
        var response = pixPaymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{txid}/confirm")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    @Operation(summary = "Conciliar cobrança Pix",
            description = "Confirma o pagamento Pix pelo txid, registra a baixa (método PIX), liquida o recebível e registra a movimentação no histórico.")
    public ResponseEntity<PixPaymentResponse> confirm(@PathVariable String txid) {
        return ResponseEntity.ok(pixPaymentService.confirm(txid));
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Listar cobranças Pix",
            description = "Lista as cobranças Pix do estúdio corrente (paginado, por data de criação decrescente).")
    public ResponseEntity<Page<PixPaymentResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(pixPaymentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    @Operation(summary = "Buscar cobrança Pix por ID")
    public ResponseEntity<PixPaymentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(pixPaymentService.findById(id));
    }
}
