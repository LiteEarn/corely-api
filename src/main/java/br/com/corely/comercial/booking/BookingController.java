package br.com.corely.comercial.booking;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.booking.dto.BookingRequest;
import br.com.corely.comercial.booking.dto.BookingResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
