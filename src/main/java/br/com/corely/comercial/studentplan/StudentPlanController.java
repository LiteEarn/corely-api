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
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<List<StudentPlanResponse>> findAll() {
        var response = studentPlanService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<StudentPlanResponse> findById(@PathVariable UUID id) {
        var response = studentPlanService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<StudentPlanResponse> cancel(@PathVariable UUID id) {
        var response = studentPlanService.cancel(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/suspend")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<StudentPlanResponse> suspend(@PathVariable UUID id) {
        var response = studentPlanService.suspend(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reactivate")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    public ResponseEntity<StudentPlanResponse> reactivate(@PathVariable UUID id) {
        var response = studentPlanService.reactivate(id);
        return ResponseEntity.ok(response);
    }
}
