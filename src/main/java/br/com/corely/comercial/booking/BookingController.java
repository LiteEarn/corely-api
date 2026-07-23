package br.com.corely.comercial.booking;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.booking.dto.*;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController("comercialBookingController")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/comercial/bookings")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        var response = bookingService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/comercial/bookings")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<Page<BookingResponse>> findAll(
            @RequestParam(required = false) UUID classSessionId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) BookingStatus status,
            Pageable pageable) {
        var response = bookingService.findAll(classSessionId, studentId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comercial/bookings/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<BookingResponse> findById(@PathVariable UUID id) {
        var response = bookingService.findById(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comercial/bookings/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/comercial/bookings/agenda")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<Page<BookingResponse>> findAgenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) UUID studentId,
            Pageable pageable) {
        var response = bookingService.findByAgenda(startDate, endDate, instructorId, roomId, studentId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comercial/bookings/{id}/conflicts")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<List<ConflictResponse>> findConflicts(@PathVariable UUID id) {
        var response = bookingService.findConflicts(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comercial/bookings/availability")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<List<AvailabilityResponse>> findAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) Long roomId) {
        var response = bookingService.findAvailability(date, instructorId, roomId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/comercial/bookings/{id}/confirm")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> confirm(@PathVariable UUID id) {
        bookingService.confirm(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/comercial/bookings/{id}/cancel")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> cancel(@PathVariable UUID id,
                                       @Valid @RequestBody CancelBookingRequest request) {
        bookingService.cancel(id, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/comercial/bookings/{id}/reschedule")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> reschedule(@PathVariable UUID id,
                                           @Valid @RequestBody RescheduleBookingRequest request) {
        bookingService.reschedule(id, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/comercial/bookings/{id}/no-show")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> noShow(@PathVariable UUID id) {
        bookingService.markNoShow(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/comercial/bookings/{id}/complete")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> complete(@PathVariable UUID id) {
        bookingService.complete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/comercial/bookings/dashboard")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<BookingDashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var response = bookingService.getDashboard(date);
        return ResponseEntity.ok(response);
    }
}
