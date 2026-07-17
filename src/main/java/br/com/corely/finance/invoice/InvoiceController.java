package br.com.corely.finance.invoice;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.finance.invoice.dto.DashboardResponse;
import br.com.corely.finance.invoice.dto.GenerateInvoiceRequest;
import br.com.corely.finance.invoice.dto.GenerateInvoiceResponse;
import br.com.corely.finance.invoice.dto.InvoiceRequest;
import br.com.corely.finance.invoice.dto.InvoiceResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController("financeInvoiceController")
@RequestMapping("/finance/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceGenerationService invoiceGenerationService;
    private final ComercialTenantContext tenantContext;

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

    @PostMapping("/{id}/pay")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<InvoiceResponse> pay(@PathVariable UUID id) {
        var response = invoiceService.pay(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<InvoiceResponse> cancel(@PathVariable UUID id) {
        var response = invoiceService.cancel(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<GenerateInvoiceResponse> generate(@Valid @RequestBody GenerateInvoiceRequest request) {
        var studioId = tenantContext.getCurrentStudioId();
        var response = invoiceGenerationService.generate(request, studioId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<DashboardResponse> dashboard(@RequestParam String month) {
        var studioId = tenantContext.getCurrentStudioId();
        var response = invoiceGenerationService.dashboard(month, studioId);
        return ResponseEntity.ok(response);
    }
}
