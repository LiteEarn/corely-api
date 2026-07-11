package br.com.corely.planenrollment;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.planenrollment.dto.PlanEnrollmentRequest;
import br.com.corely.planenrollment.dto.PlanEnrollmentResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/plan-enrollments")
@RequiredArgsConstructor
public class PlanEnrollmentController {

    private final PlanEnrollmentService planEnrollmentService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<PlanEnrollmentResponse> create(@Valid @RequestBody PlanEnrollmentRequest request) {
        PlanEnrollmentResponse response = planEnrollmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<List<PlanEnrollmentResponse>> findAll() {
        List<PlanEnrollmentResponse> response = planEnrollmentService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<PlanEnrollmentResponse> findById(@PathVariable UUID id) {
        PlanEnrollmentResponse response = planEnrollmentService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<PlanEnrollmentResponse> cancel(@PathVariable UUID id) {
        PlanEnrollmentResponse response = planEnrollmentService.cancel(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<List<PlanEnrollmentResponse>> findByStudent(@PathVariable UUID studentId) {
        List<PlanEnrollmentResponse> response = planEnrollmentService.findByStudentId(studentId);
        return ResponseEntity.ok(response);
    }
}
