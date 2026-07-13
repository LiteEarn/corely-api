package br.com.corely.comercial.plan;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.plan.dto.PlanRequest;
import br.com.corely.comercial.plan.dto.PlanResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/comercial/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody PlanRequest request) {
        var response = planService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<Page<PlanResponse>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean active,
            Pageable pageable) {
        var response = planService.findAll(name, active, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<PlanResponse> findById(@PathVariable UUID id) {
        var response = planService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<PlanResponse> update(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        var response = planService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        planService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/inactivate")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        planService.inactivate(id);
        return ResponseEntity.noContent().build();
    }
}
