package br.com.corely.plan;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.plan.dto.PlanRequest;
import br.com.corely.plan.dto.PlanResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<PlanResponse> create(@Valid @RequestBody PlanRequest request) {
        PlanResponse response = planService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<List<PlanResponse>> findAll() {
        List<PlanResponse> response = planService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<PlanResponse> findById(@PathVariable UUID id) {
        PlanResponse response = planService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<PlanResponse> update(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        PlanResponse response = planService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        planService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/inactivate")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        planService.inactivate(id);
        return ResponseEntity.noContent().build();
    }
}
