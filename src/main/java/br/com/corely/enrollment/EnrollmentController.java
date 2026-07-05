package br.com.corely.enrollment;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.enrollment.dto.EnrollmentRequest;
import br.com.corely.enrollment.dto.EnrollmentResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @RequireRole({UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<EnrollmentResponse> create(@Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<List<EnrollmentResponse>> findAll() {
        List<EnrollmentResponse> response = enrollmentService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<EnrollmentResponse> findById(@PathVariable UUID id) {
        EnrollmentResponse response = enrollmentService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @RequireRole({UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<EnrollmentResponse> update(@PathVariable UUID id, @Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @RequireRole({UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        enrollmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/class-groups/{classGroupId}/students")
    @RequireRole({UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<List<EnrollmentResponse>> findStudentsByClassGroupId(@PathVariable UUID classGroupId) {
        List<EnrollmentResponse> response = enrollmentService.findStudentsByClassGroupId(classGroupId);
        return ResponseEntity.ok(response);
    }
}
