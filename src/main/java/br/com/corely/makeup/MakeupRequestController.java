package br.com.corely.makeup;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.makeup.dto.MakeupApproveRequest;
import br.com.corely.makeup.dto.MakeupRejectRequest;
import br.com.corely.makeup.dto.MakeupRequestRequest;
import br.com.corely.makeup.dto.MakeupRequestResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MakeupRequestController {

    private final MakeupRequestService makeupRequestService;

    @PostMapping("/attendance/{attendanceId}/makeup-request")
    @RequireRole({UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<MakeupRequestResponse> request(
            @PathVariable UUID attendanceId,
            @Valid @RequestBody MakeupRequestRequest request) {
        MakeupRequestResponse response = makeupRequestService.request(attendanceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/attendance/{attendanceId}/makeup-request")
    @RequireRole({UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<MakeupRequestResponse> findByAttendance(@PathVariable UUID attendanceId) {
        MakeupRequestResponse response = makeupRequestService.findByAttendanceId(attendanceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/makeup-requests")
    @RequireRole({UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<List<MakeupRequestResponse>> findAll(
            @RequestParam(required = false) MakeupRequestStatus status,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID classGroupId) {
        List<MakeupRequestResponse> response = makeupRequestService.findAll(status, studentId, classGroupId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/makeup-requests/{id}/approve")
    @RequireRole({UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<MakeupRequestResponse> approve(
            @PathVariable UUID id,
            @Valid @RequestBody MakeupApproveRequest request) {
        MakeupRequestResponse response = makeupRequestService.approve(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/makeup-requests/{id}/reject")
    @RequireRole({UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<MakeupRequestResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody MakeupRejectRequest request) {
        MakeupRequestResponse response = makeupRequestService.reject(id, request);
        return ResponseEntity.ok(response);
    }
}
