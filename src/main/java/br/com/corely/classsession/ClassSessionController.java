package br.com.corely.classsession;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.classsession.dto.ClassSessionRequest;
import br.com.corely.classsession.dto.ClassSessionResponse;
import br.com.corely.classsession.dto.DailyScheduleResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/class-sessions")
@RequiredArgsConstructor
public class ClassSessionController {

    private final ClassSessionService classSessionService;

    @PostMapping
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<ClassSessionResponse> create(@Valid @RequestBody ClassSessionRequest request) {
        ClassSessionResponse response = classSessionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<List<ClassSessionResponse>> findAll(
            @RequestParam(required = false) UUID classGroupId,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) ClassSessionStatus status,
            @RequestParam(required = false) LocalDate sessionDate) {
        List<ClassSessionResponse> response = classSessionService.findAll(classGroupId, instructorId, status, sessionDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/daily")
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<DailyScheduleResponse> getDailySchedule(
            @RequestParam UUID studioId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) ClassSessionStatus status,
            @RequestParam(required = false) UUID classGroupId) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        DailyScheduleResponse response = classSessionService.getDailySchedule(studioId, targetDate, instructorId, status, classGroupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<ClassSessionResponse> findById(@PathVariable UUID id) {
        ClassSessionResponse response = classSessionService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/start")
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> start(@PathVariable UUID id) {
        classSessionService.start(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/complete")
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> complete(@PathVariable UUID id) {
        classSessionService.complete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel")
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        classSessionService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
