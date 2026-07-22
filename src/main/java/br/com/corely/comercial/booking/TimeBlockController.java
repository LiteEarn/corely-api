package br.com.corely.comercial.booking;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.booking.dto.TimeBlockRequest;
import br.com.corely.comercial.booking.dto.TimeBlockResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TimeBlockController {

    private final TimeBlockService timeBlockService;

    @PostMapping("/comercial/time-blocks")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<TimeBlockResponse> create(@Valid @RequestBody TimeBlockRequest request) {
        var response = timeBlockService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/comercial/time-blocks")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<Page<TimeBlockResponse>> findAll(
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            Pageable pageable) {
        var response = timeBlockService.findAll(instructorId, roomId, startDate, endDate, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comercial/time-blocks/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<TimeBlockResponse> findById(@PathVariable UUID id) {
        var response = timeBlockService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/comercial/time-blocks/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<TimeBlockResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody TimeBlockRequest request) {
        var response = timeBlockService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comercial/time-blocks/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        timeBlockService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
