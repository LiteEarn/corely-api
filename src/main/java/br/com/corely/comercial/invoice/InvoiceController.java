package br.com.corely.comercial.invoice;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.invoice.dto.InvoiceRequest;
import br.com.corely.comercial.invoice.dto.InvoiceResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comercial/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        var response = invoiceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    public ResponseEntity<List<InvoiceResponse>> findAll() {
        var response = invoiceService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    public ResponseEntity<InvoiceResponse> findById(@PathVariable UUID id) {
        var response = invoiceService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<InvoiceResponse> cancel(@PathVariable UUID id) {
        var response = invoiceService.cancel(id);
        return ResponseEntity.ok(response);
    }
}
