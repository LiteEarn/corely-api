package br.com.corely.booking;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.booking.dto.*;
import br.com.corely.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "API for studio agenda management")
public class BookingController {

    private final BookingService bookingService;
    private final TimeBlockService timeBlockService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    @Operation(summary = "Create a booking")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        var response = bookingService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.INSTRUCTOR})
    @Operation(summary = "Find booking by ID")
    public ResponseEntity<BookingResponse> findById(@PathVariable UUID id) {
        var response = bookingService.findById(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    @Operation(summary = "Delete a booking")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/agenda")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.INSTRUCTOR})
    @Operation(summary = "List agenda by date range")
    public ResponseEntity<List<BookingResponse>> getAgenda(
            @RequestParam UUID studioId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        var response = bookingService.findAgenda(studioId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/conflicts")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    @Operation(summary = "List conflicts for a booking")
    public ResponseEntity<List<ConflictResponse>> getConflicts(@PathVariable UUID id) {
        var response = bookingService.findConflicts(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/availability")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    @Operation(summary = "Check availability for instructor/room on a date")
    public ResponseEntity<List<AvailabilityResponse>> getAvailability(
            @RequestParam UUID studioId,
            @RequestParam UUID instructorId,
            @RequestParam(required = false) Long roomId,
            @RequestParam LocalDate date) {
        var response = bookingService.findAvailability(studioId, instructorId, roomId, date);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/confirm")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    @Operation(summary = "Confirm a booking")
    public ResponseEntity<BookingResponse> confirm(@PathVariable UUID id) {
        var response = bookingService.confirm(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<BookingResponse> cancel(
            @PathVariable UUID id,
            @RequestParam CancellationReason reason,
            @RequestParam(required = false, defaultValue = "") String notes) {
        var response = bookingService.cancel(id, reason, notes);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reschedule")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    @Operation(summary = "Reschedule a booking")
    public ResponseEntity<BookingResponse> reschedule(
            @PathVariable UUID id,
            @RequestParam LocalDateTime startDateTime,
            @RequestParam LocalDateTime endDateTime) {
        var response = bookingService.reschedule(id, startDateTime, endDateTime);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/no-show")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    @Operation(summary = "Mark booking as no-show")
    public ResponseEntity<BookingResponse> markNoShow(@PathVariable UUID id) {
        var response = bookingService.markNoShow(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/complete")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    @Operation(summary = "Mark booking as completed")
    public ResponseEntity<BookingResponse> markCompleted(@PathVariable UUID id) {
        var response = bookingService.markCompleted(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    @Operation(summary = "Get booking dashboard metrics")
    public ResponseEntity<DashboardBookingMetricsResponse> getDashboardMetrics(
            @RequestParam UUID studioId) {
        var response = bookingService.getDashboardMetrics(studioId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/time-blocks")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    @Operation(summary = "Create a time block")
    public ResponseEntity<TimeBlockResponse> createTimeBlock(@Valid @RequestBody TimeBlockRequest request) {
        var response = timeBlockService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/time-blocks")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    @Operation(summary = "List active time blocks")
    public ResponseEntity<List<TimeBlockResponse>> getTimeBlocks(
            @RequestParam UUID studioId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        var response = timeBlockService.findActiveBlocks(studioId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/time-blocks/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN})
    @Operation(summary = "Delete a time block")
    public ResponseEntity<Void> deleteTimeBlock(@PathVariable UUID id) {
        timeBlockService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
