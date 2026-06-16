package br.com.corely.attendance;

import br.com.corely.attendance.dto.AttendanceBulkRequest;
import br.com.corely.attendance.dto.AttendanceRequest;
import br.com.corely.attendance.dto.AttendanceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private static final Logger log = LoggerFactory.getLogger(AttendanceController.class);

    private final AttendanceService attendanceService;

    @GetMapping
    public ResponseEntity<List<AttendanceResponse>> findAll() {
        log.info("AttendanceController.findAll - Request received");
        List<AttendanceResponse> response = attendanceService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttendanceResponse> findById(@PathVariable UUID id) {
        log.info("AttendanceController.findById - Request received for id: {}", id);
        AttendanceResponse response = attendanceService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<AttendanceResponse> create(@Valid @RequestBody AttendanceRequest request) {
        log.info("AttendanceController.create - Request received");
        AttendanceResponse response = attendanceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttendanceResponse> update(@PathVariable UUID id, @Valid @RequestBody AttendanceRequest request) {
        log.info("AttendanceController.update - Request received for id: {}", id);
        AttendanceResponse response = attendanceService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("AttendanceController.delete - Request received for id: {}", id);
        attendanceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/class-group/{classGroupId}")
    public ResponseEntity<List<AttendanceResponse>> findByClassGroupId(@PathVariable UUID classGroupId) {
        log.info("AttendanceController.findByClassGroupId - Request received for classGroupId: {}", classGroupId);
        List<AttendanceResponse> response = attendanceService.findByClassGroupId(classGroupId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/class-group/{classGroupId}/date/{attendanceDate}")
    public ResponseEntity<List<AttendanceResponse>> findByClassGroupIdAndAttendanceDate(
            @PathVariable UUID classGroupId,
            @PathVariable String attendanceDate) {
        log.info("AttendanceController.findByClassGroupIdAndAttendanceDate - Request received for classGroupId: {}, date: {}", classGroupId, attendanceDate);
        List<AttendanceResponse> response = attendanceService.findByClassGroupIdAndAttendanceDate(classGroupId, attendanceDate);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<AttendanceResponse>> createBulk(@Valid @RequestBody AttendanceBulkRequest request) {
        log.debug("AttendanceController.createBulk - Request received");
        List<AttendanceResponse> response = attendanceService.createBulk(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
