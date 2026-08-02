package br.com.corely.dashboard;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.dashboard.dto.DashboardResponse;
import br.com.corely.dashboard.operational.dto.DashboardOperationalResponse;
import br.com.corely.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "API for dashboard metrics and statistics")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    @GetMapping
    @RequireRole({UserRole.ADMIN, UserRole.OWNER})
    @Operation(summary = "Get dashboard metrics", description = "Retrieves dashboard metrics for the authenticated studio")
    public ResponseEntity<DashboardResponse> getDashboard() {
        log.info("ENTERED DASHBOARD CONTROLLER - getDashboard");
        DashboardResponse response = dashboardService.getDashboard();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operational")
    @RequireRole({UserRole.ADMIN, UserRole.OWNER, UserRole.RECEPTIONIST, UserRole.INSTRUCTOR})
    @Operation(summary = "Get operational dashboard", description = "Retorna dados agregados do Dashboard Operacional para o studio autenticado")
    public ResponseEntity<DashboardOperationalResponse> getOperationalDashboard() {
        DashboardOperationalResponse response = dashboardService.getOperationalDashboard();
        return ResponseEntity.ok(response);
    }
}
