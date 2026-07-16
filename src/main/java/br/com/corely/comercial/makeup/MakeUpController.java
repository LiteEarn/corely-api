package br.com.corely.comercial.makeup;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.makeup.dto.MakeUpCreditRequest;
import br.com.corely.comercial.makeup.dto.MakeUpCreditResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController("comercialMakeUpController")
@RequiredArgsConstructor
public class MakeUpController {

    private final MakeUpService makeUpService;

    @GetMapping("/comercial/makeup-credits")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<Page<MakeUpCreditResponse>> findAll(
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) MakeUpCreditStatus status,
            Pageable pageable) {
        var response = makeUpService.findAll(studentId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comercial/makeup-credits/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<MakeUpCreditResponse> findById(@PathVariable UUID id) {
        var response = makeUpService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/comercial/makeup-credits/{id}/use")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<MakeUpCreditResponse> use(@PathVariable UUID id,
                                                     @Valid @RequestBody MakeUpCreditRequest request) {
        var response = makeUpService.use(id, request);
        return ResponseEntity.ok(response);
    }
}
