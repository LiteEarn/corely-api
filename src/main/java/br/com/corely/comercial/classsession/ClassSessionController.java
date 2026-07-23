package br.com.corely.comercial.classsession;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.classsession.dto.CancelSessionRequest;
import br.com.corely.comercial.classsession.dto.ClassSessionRequest;
import br.com.corely.comercial.classsession.dto.ClassSessionResponse;
import br.com.corely.comercial.classsession.dto.SessionStatusDto;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController("comercialClassSessionController")
@RequiredArgsConstructor
public class ClassSessionController {

    private final ClassSessionService classSessionService;

    @PostMapping("/comercial/class-sessions")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<ClassSessionResponse> create(@Valid @RequestBody ClassSessionRequest request) {
        var response = classSessionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/comercial/class-sessions")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<Page<ClassSessionResponse>> findAll(
            @RequestParam(required = false) UUID scheduleSlotId,
            @RequestParam(required = false) SessionStatusDto status,
            Pageable pageable) {
        var response = classSessionService.findAll(scheduleSlotId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comercial/class-sessions/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<ClassSessionResponse> findById(@PathVariable UUID id) {
        var response = classSessionService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/comercial/class-sessions/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<ClassSessionResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody ClassSessionRequest request) {
        var response = classSessionService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/comercial/class-sessions/{id}/cancel")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<ClassSessionResponse> cancel(@PathVariable UUID id,
                                                       @Valid @RequestBody CancelSessionRequest request) {
        var response = classSessionService.cancelSession(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/comercial/class-sessions/{id}/start")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<ClassSessionResponse> start(@PathVariable UUID id) {
        var response = classSessionService.startSession(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/comercial/class-sessions/{id}/finish")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<ClassSessionResponse> finish(@PathVariable UUID id) {
        var response = classSessionService.finishSession(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comercial/class-sessions/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        classSessionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
