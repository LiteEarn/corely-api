package br.com.corely.dashboard;

import br.com.corely.dashboard.dto.DashboardResponse;
import br.com.corely.dashboard.operational.dto.DashboardOperationalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "API for dashboard metrics and statistics")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get dashboard metrics", description = "Retrieves dashboard metrics for a specific studio")
    public ResponseEntity<DashboardResponse> getDashboard(
            @Parameter(description = "Studio ID", required = true)
            @RequestParam UUID studioId) {
        log.info("ENTERED DASHBOARD CONTROLLER - getDashboard with studioId: {}", studioId);
        DashboardResponse response = dashboardService.getDashboard(studioId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/operational")
    @Operation(summary = "Get operational dashboard", description = "Retorna dados agregados do Dashboard Operacional")
    public ResponseEntity<DashboardOperationalResponse> getOperationalDashboard(
            @Parameter(description = "Studio ID (opcional - usa o primeiro studio ativo quando não informado)")
            @RequestParam(required = false) UUID studioId) {
        DashboardOperationalResponse response = dashboardService.getOperationalDashboard(studioId);
        return ResponseEntity.ok(response);
    }
}
