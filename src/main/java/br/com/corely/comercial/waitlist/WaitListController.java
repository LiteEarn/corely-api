package br.com.corely.comercial.waitlist;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.waitlist.dto.WaitListRequest;
import br.com.corely.comercial.waitlist.dto.WaitListResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController("comercialWaitListController")
@RequiredArgsConstructor
public class WaitListController {

    private final WaitListService waitListService;

    @PostMapping("/comercial/wait-list")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<WaitListResponse> create(@Valid @RequestBody WaitListRequest request) {
        var response = waitListService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/comercial/wait-list")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<Page<WaitListResponse>> findAll(
            @RequestParam(required = false) UUID classSessionId,
            Pageable pageable) {
        var response = waitListService.findAll(classSessionId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/comercial/wait-list/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    public ResponseEntity<WaitListResponse> findById(@PathVariable UUID id) {
        var response = waitListService.findById(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comercial/wait-list/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST})
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        waitListService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
