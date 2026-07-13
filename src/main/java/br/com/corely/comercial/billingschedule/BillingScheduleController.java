package br.com.corely.comercial.billingschedule;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.billingschedule.dto.BillingScheduleRequest;
import br.com.corely.comercial.billingschedule.dto.BillingScheduleResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comercial/billing-schedules")
@RequiredArgsConstructor
public class BillingScheduleController {

    private final BillingScheduleService billingScheduleService;

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    public ResponseEntity<List<BillingScheduleResponse>> findAll() {
        var response = billingScheduleService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    public ResponseEntity<BillingScheduleResponse> findById(@PathVariable UUID id) {
        var response = billingScheduleService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<BillingScheduleResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody BillingScheduleRequest request) {
        var response = billingScheduleService.update(id, request);
        return ResponseEntity.ok(response);
    }
}
