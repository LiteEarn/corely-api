package br.com.corely.comercial.delinquencypolicy;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.delinquencypolicy.dto.DelinquencyPolicyRequest;
import br.com.corely.comercial.delinquencypolicy.dto.DelinquencyPolicyResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comercial/delinquency-policy")
@RequiredArgsConstructor
public class DelinquencyPolicyController {

    private final DelinquencyPolicyService delinquencyPolicyService;

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<DelinquencyPolicyResponse> get() {
        var response = delinquencyPolicyService.getOrCreate();
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<DelinquencyPolicyResponse> update(@Valid @RequestBody DelinquencyPolicyRequest request) {
        var response = delinquencyPolicyService.update(request);
        return ResponseEntity.ok(response);
    }
}
