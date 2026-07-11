package br.com.corely.attendance;

import br.com.corely.attendance.dto.AttendanceRequest;
import br.com.corely.attendance.dto.AttendanceResponse;
import br.com.corely.attendance.dto.BulkAttendanceRequest;
import br.com.corely.attendance.dto.BulkAttendanceResponse;
import br.com.corely.attendance.dto.SessionAttendanceResponse;
import br.com.corely.attendance.dto.SessionBulkAttendanceRequest;
import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.user.User;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/class-sessions/{sessionId}/attendance")
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<AttendanceResponse> register(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AttendanceRequest request) {
        AttendanceResponse response = attendanceService.register(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/attendance/bulk")
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<BulkAttendanceResponse> bulkSave(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BulkAttendanceRequest request) {
        request.setStudioId(user.getStudio().getId());
        BulkAttendanceResponse response = attendanceService.bulkSave(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/class-sessions/{sessionId}/attendance")
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<List<AttendanceResponse>> findBySession(@PathVariable UUID sessionId) {
        List<AttendanceResponse> response = attendanceService.findBySessionId(sessionId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/class-sessions/{sessionId}/attendance")
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<List<SessionAttendanceResponse>> saveSessionAttendances(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SessionBulkAttendanceRequest request) {
        List<SessionAttendanceResponse> responses = attendanceService.saveSessionAttendances(sessionId, request);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/enrollments/{enrollmentId}/attendance")
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<List<AttendanceResponse>> findByEnrollment(@PathVariable UUID enrollmentId) {
        List<AttendanceResponse> response = attendanceService.findByEnrollmentId(enrollmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/attendance/class-group/{classGroupId}/date/{date}")
    @RequireRole({UserRole.ADMIN, UserRole.INSTRUCTOR, UserRole.RECEPTIONIST})
    public ResponseEntity<List<AttendanceResponse>> findByClassGroupAndDate(
            @PathVariable UUID classGroupId,
            @PathVariable LocalDate date) {
        List<AttendanceResponse> response = attendanceService.findByClassGroupAndDate(classGroupId, date);
        return ResponseEntity.ok(response);
    }
}
