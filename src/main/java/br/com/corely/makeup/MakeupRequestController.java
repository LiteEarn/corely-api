package br.com.corely.makeup;

import br.com.corely.makeup.dto.MakeupRequestRequest;
import br.com.corely.makeup.dto.MakeupRequestResponse;
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
    public ResponseEntity<MakeupRequestResponse> request(
            @PathVariable UUID attendanceId,
            @Valid @RequestBody MakeupRequestRequest request) {
        MakeupRequestResponse response = makeupRequestService.request(attendanceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/attendance/{attendanceId}/makeup-request")
    public ResponseEntity<MakeupRequestResponse> findByAttendance(@PathVariable UUID attendanceId) {
        MakeupRequestResponse response = makeupRequestService.findByAttendanceId(attendanceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/makeup-requests")
    public ResponseEntity<List<MakeupRequestResponse>> findAll(
            @RequestParam(required = false) MakeupRequestStatus status,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID classGroupId) {
        List<MakeupRequestResponse> response = makeupRequestService.findAll(status, studentId, classGroupId);
        return ResponseEntity.ok(response);
    }
}
