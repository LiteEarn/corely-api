package br.com.corely.comercial.studentplan;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.studentplan.dto.StudentPlanRequest;
import br.com.corely.comercial.studentplan.dto.StudentPlanResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comercial/student-plans")
@RequiredArgsConstructor
public class StudentPlanController {

    private final StudentPlanService studentPlanService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<StudentPlanResponse> create(@Valid @RequestBody StudentPlanRequest request) {
        var response = studentPlanService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<List<StudentPlanResponse>> findAll() {
        var response = studentPlanService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<StudentPlanResponse> findById(@PathVariable UUID id) {
        var response = studentPlanService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<StudentPlanResponse> cancel(@PathVariable UUID id) {
        var response = studentPlanService.cancel(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<List<StudentPlanResponse>> findByStudent(@PathVariable UUID studentId) {
        var response = studentPlanService.findByStudentId(studentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}/active")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<StudentPlanResponse> findActiveByStudent(@PathVariable UUID studentId) {
        var response = studentPlanService.findActiveByStudent(studentId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}
