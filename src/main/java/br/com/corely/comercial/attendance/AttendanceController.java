package br.com.corely.comercial.attendance;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.attendance.dto.AttendanceRequest;
import br.com.corely.comercial.attendance.dto.AttendanceResponse;
import br.com.corely.comercial.attendance.dto.BulkAttendanceRequest;
import br.com.corely.comercial.attendance.dto.BulkAttendanceResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController("comercialAttendanceController")
@RequestMapping("/comercial/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public AttendanceResponse register(@PathVariable UUID sessionId,
                                       @Valid @RequestBody AttendanceRequest request) {
        return attendanceService.register(sessionId, request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public BulkAttendanceResponse bulkSave(@Valid @RequestBody BulkAttendanceRequest request) {
        return attendanceService.bulkSave(request);
    }

    @GetMapping("/sessions/{sessionId}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public Page<AttendanceResponse> findBySessionId(@PathVariable UUID sessionId, Pageable pageable) {
        return attendanceService.findBySessionId(sessionId, pageable);
    }

    @GetMapping("/bookings/{bookingId}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public Page<AttendanceResponse> findByBookingId(@PathVariable UUID bookingId, Pageable pageable) {
        return attendanceService.findByBookingId(bookingId, pageable);
    }

    @GetMapping("/students/{studentId}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public Page<AttendanceResponse> findByStudentId(@PathVariable UUID studentId, Pageable pageable) {
        return attendanceService.findByStudentId(studentId, pageable);
    }

    @GetMapping("/schedules/{scheduleId}/date/{date}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public List<AttendanceResponse> findByScheduleAndDate(
            @PathVariable UUID scheduleId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.findByScheduleAndDate(scheduleId, date);
    }
}
