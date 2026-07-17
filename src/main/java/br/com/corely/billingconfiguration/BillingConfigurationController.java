package br.com.corely.billingconfiguration;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.billingconfiguration.dto.BillingConfigurationRequest;
import br.com.corely.billingconfiguration.dto.BillingConfigurationResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/finance/billing-configuration")
@RequiredArgsConstructor
public class BillingConfigurationController {

    private final BillingConfigurationService billingConfigurationService;

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<BillingConfigurationResponse> findByCurrentStudio() {
        var response = billingConfigurationService.findByCurrentStudio();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<BillingConfigurationResponse> save(@Valid @RequestBody BillingConfigurationRequest request) {
        var response = billingConfigurationService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
