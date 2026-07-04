package br.com.corely.attendance;

import br.com.corely.attendance.dto.AttendanceRequest;
import br.com.corely.attendance.dto.AttendanceResponse;
import br.com.corely.attendance.dto.AttendanceBulkItem;
import br.com.corely.attendance.dto.BulkAttendanceResponse;
import br.com.corely.user.User;
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
    public ResponseEntity<AttendanceResponse> register(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AttendanceRequest request) {
        AttendanceResponse response = attendanceService.register(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/attendance/bulk")
    public ResponseEntity<BulkAttendanceResponse> bulkSave(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody List<@Valid AttendanceBulkItem> items) {
        BulkAttendanceResponse response = attendanceService.bulkSave(user.getStudio().getId(), items);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/class-sessions/{sessionId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> findBySession(@PathVariable UUID sessionId) {
        List<AttendanceResponse> response = attendanceService.findBySessionId(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/enrollments/{enrollmentId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> findByEnrollment(@PathVariable UUID enrollmentId) {
        List<AttendanceResponse> response = attendanceService.findByEnrollmentId(enrollmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/attendance/class-group/{classGroupId}/date/{date}")
    public ResponseEntity<List<AttendanceResponse>> findByClassGroupAndDate(
            @PathVariable UUID classGroupId,
            @PathVariable LocalDate date) {
        List<AttendanceResponse> response = attendanceService.findByClassGroupAndDate(classGroupId, date);
        return ResponseEntity.ok(response);
    }
}
