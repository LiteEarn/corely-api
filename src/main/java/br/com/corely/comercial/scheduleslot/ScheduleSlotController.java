package br.com.corely.comercial.scheduleslot;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.scheduleslot.dto.ScheduleSlotRequest;
import br.com.corely.comercial.scheduleslot.dto.ScheduleSlotResponse;
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
public class ScheduleSlotController {

    private final ScheduleSlotService scheduleSlotService;

    @PostMapping("/comercial/schedules/{scheduleId}/slots")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<ScheduleSlotResponse> create(@PathVariable UUID scheduleId,
                                                       @Valid @RequestBody ScheduleSlotRequest request) {
        var response = scheduleSlotService.create(scheduleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/comercial/schedules/{scheduleId}/slots")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<List<ScheduleSlotResponse>> findByScheduleId(@PathVariable UUID scheduleId) {
        var response = scheduleSlotService.findByScheduleId(scheduleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comercial/schedule-slots/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<ScheduleSlotResponse> findById(@PathVariable UUID id) {
        var response = scheduleSlotService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/comercial/schedule-slots/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<ScheduleSlotResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody ScheduleSlotRequest request) {
        var response = scheduleSlotService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comercial/schedule-slots/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        scheduleSlotService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
