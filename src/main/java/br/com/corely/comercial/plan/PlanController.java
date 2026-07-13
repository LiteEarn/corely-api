package br.com.corely.comercial.plan;

import br.com.corely.auth.authorization.Permission;
import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.plan.dto.PlanRequest;
import br.com.corely.comercial.plan.dto.PlanResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
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
    public ResponseEntity<List<PlanResponse>> findAll(@RequestParam("active") Optional<Boolean> active) {
        var response = active.isPresent() && active.get()
                ? planService.findAllActive()
                : planService.findAll();
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
