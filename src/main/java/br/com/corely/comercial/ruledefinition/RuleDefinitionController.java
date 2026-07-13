package br.com.corely.comercial.ruledefinition;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.ruledefinition.dto.RuleDefinitionRequest;
import br.com.corely.comercial.ruledefinition.dto.RuleDefinitionResponse;
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
@RequestMapping("/comercial/rule-definitions")
@RequiredArgsConstructor
public class RuleDefinitionController {

    private final RuleDefinitionService service;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<RuleDefinitionResponse> create(@Valid @RequestBody RuleDefinitionRequest request) {
        var response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<List<RuleDefinitionResponse>> findAll(@RequestParam("active") Optional<Boolean> active) {
        var response = active.isPresent() && active.get()
                ? service.findAllActive()
                : service.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/all")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<List<RuleDefinitionResponse>> findAllAdmin() {
        var response = service.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<RuleDefinitionResponse> findById(@PathVariable UUID id) {
        var response = service.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<RuleDefinitionResponse> update(@PathVariable UUID id, @Valid @RequestBody RuleDefinitionRequest request) {
        var response = service.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/inactivate")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        service.inactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        service.activate(id);
        return ResponseEntity.noContent().build();
    }
}
