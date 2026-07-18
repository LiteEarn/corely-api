package br.com.corely.finance.membershipplan;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.finance.membershipplan.dto.MembershipPlanRequest;
import br.com.corely.finance.membershipplan.dto.MembershipPlanResponse;
import br.com.corely.user.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController("financeMembershipPlanController")
@RequestMapping("/finance/plans")
@RequiredArgsConstructor
public class MembershipPlanController {

    private final MembershipPlanService membershipPlanService;

    @PostMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<MembershipPlanResponse> create(@Valid @RequestBody MembershipPlanRequest request) {
        var response = membershipPlanService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    public ResponseEntity<List<MembershipPlanResponse>> findAll() {
        var response = membershipPlanService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL, UserRole.RECEPTIONIST})
    public ResponseEntity<MembershipPlanResponse> findById(@PathVariable UUID id) {
        var response = membershipPlanService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<MembershipPlanResponse> update(@PathVariable UUID id, @Valid @RequestBody MembershipPlanRequest request) {
        var response = membershipPlanService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        membershipPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
