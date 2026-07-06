package br.com.corely.classgroup;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.classgroup.dto.ClassGroupRequest;
import br.com.corely.classgroup.dto.ClassGroupResponse;
import br.com.corely.classgroup.dto.ConfirmInactivationRequest;
import br.com.corely.classsession.ClassSessionService;
import br.com.corely.classsession.dto.SessionGenerationResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/class-groups")
@RequiredArgsConstructor
public class ClassGroupController {

    private final ClassGroupService classGroupService;
    private final ClassSessionService classSessionService;

    @PostMapping
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<ClassGroupResponse> create(@Valid @RequestBody ClassGroupRequest request) {
        ClassGroupResponse response = classGroupService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<List<ClassGroupResponse>> findAll() {
        List<ClassGroupResponse> response = classGroupService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<List<ClassGroupResponse>> findActive() {
        List<ClassGroupResponse> response = classGroupService.findActive();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<ClassGroupResponse> findById(@PathVariable UUID id) {
        ClassGroupResponse response = classGroupService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<ClassGroupResponse> update(@PathVariable UUID id, @Valid @RequestBody ClassGroupRequest request) {
        ClassGroupResponse response = classGroupService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        classGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/inactivate")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<Void> inactivate(@PathVariable UUID id, @Valid @RequestBody ConfirmInactivationRequest request) {
        classGroupService.inactivate(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<ClassGroupResponse> reactivate(@PathVariable UUID id) {
        ClassGroupResponse response = classGroupService.reactivate(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/generate-sessions")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<SessionGenerationResponse> generateSessions(@PathVariable UUID id) {
        SessionGenerationResponse response = classSessionService.generateSessions(id);
        return ResponseEntity.ok(response);
    }
}
