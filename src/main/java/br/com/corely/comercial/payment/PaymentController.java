package br.com.corely.comercial.payment;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.payment.dto.PaymentRequest;
import br.com.corely.comercial.payment.dto.PaymentResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comercial/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request) {
        var response = paymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    public ResponseEntity<List<PaymentResponse>> findAll() {
        var response = paymentService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    public ResponseEntity<PaymentResponse> findById(@PathVariable UUID id) {
        var response = paymentService.findById(id);
        return ResponseEntity.ok(response);
    }
}
