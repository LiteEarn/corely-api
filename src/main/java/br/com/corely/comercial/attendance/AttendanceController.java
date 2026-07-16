package br.com.corely.comercial.attendance;

import br.com.corely.comercial.attendance.dto.AttendanceRequest;
import br.com.corely.comercial.attendance.dto.AttendanceResponse;
import br.com.corely.comercial.attendance.dto.BulkAttendanceRequest;
import br.com.corely.comercial.attendance.dto.BulkAttendanceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController("comercialAttendanceController")
@RequestMapping("/comercial/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceResponse register(@PathVariable UUID sessionId,
                                       @Valid @RequestBody AttendanceRequest request) {
        return attendanceService.register(sessionId, request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkAttendanceResponse bulkSave(@Valid @RequestBody BulkAttendanceRequest request) {
        return attendanceService.bulkSave(request);
    }

    @GetMapping("/sessions/{sessionId}")
    public Page<AttendanceResponse> findBySessionId(@PathVariable UUID sessionId, Pageable pageable) {
        return attendanceService.findBySessionId(sessionId, pageable);
    }

    @GetMapping("/bookings/{bookingId}")
    public Page<AttendanceResponse> findByBookingId(@PathVariable UUID bookingId, Pageable pageable) {
        return attendanceService.findByBookingId(bookingId, pageable);
    }

    @GetMapping("/students/{studentId}")
    public Page<AttendanceResponse> findByStudentId(@PathVariable UUID studentId, Pageable pageable) {
        return attendanceService.findByStudentId(studentId, pageable);
    }
}
