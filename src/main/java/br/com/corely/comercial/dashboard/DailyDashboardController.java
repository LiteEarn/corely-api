package br.com.corely.comercial.dashboard;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.comercial.dashboard.dto.DailyDashboardResponse;
import br.com.corely.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "API for dashboard metrics and statistics")
public class DailyDashboardController {

    private final DailyDashboardService dailyDashboardService;

    @GetMapping("/comercial/dashboard/daily")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.RECEPTIONIST, UserRole.FINANCIAL})
    @Operation(summary = "Get daily operational dashboard",
               description = "Retorna a visão operacional completa do dia para o dashboard. " +
                             "Se a data não for informada, utiliza a data atual.")
    public ResponseEntity<DailyDashboardResponse> getDailyDashboard(
            @Parameter(description = "Data para consulta (formato yyyy-MM-dd). Opcional.")
            @RequestParam(required = false) LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        var response = dailyDashboardService.getDailyDashboard(queryDate);
        return ResponseEntity.ok(response);
    }
}
