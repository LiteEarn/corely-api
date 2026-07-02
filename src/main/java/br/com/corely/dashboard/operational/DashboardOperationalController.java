package br.com.corely.dashboard.operational;

import br.com.corely.dashboard.operational.dto.OperationalDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Operacional", description = "Endpoint agregador do Dashboard Operacional")
public class DashboardOperationalController {

    private final DashboardOperationalService dashboardOperationalService;

    @GetMapping("/operational")
    @Operation(summary = "Get operational dashboard", description = "Retorna dados agregados do Dashboard Operacional")
    public ResponseEntity<OperationalDashboardResponse> getOperationalDashboard(
            @Parameter(description = "Studio ID", required = true)
            @RequestParam UUID studioId) {
        OperationalDashboardResponse response = dashboardOperationalService.getOperationalDashboard(studioId);
        return ResponseEntity.ok(response);
    }
}
