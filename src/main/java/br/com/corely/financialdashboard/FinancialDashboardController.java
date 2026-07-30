package br.com.corely.financialdashboard;

import br.com.corely.auth.authorization.RequireRole;
import br.com.corely.financialdashboard.dto.FinancialDashboardResponse;
import br.com.corely.user.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
@Tag(name = "Financial Dashboard", description = "API for financial dashboard indicators and metrics")
public class FinancialDashboardController {

    private final FinancialDashboardService financialDashboardService;

    @GetMapping("/dashboard")
    @RequireRole({UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCIAL})
    @Operation(summary = "Get financial dashboard",
            description = "Retorna o dashboard financeiro completo com indicadores de receita, clientes, planos, recebimentos e tendências")
    public ResponseEntity<FinancialDashboardResponse> getDashboard(
            @Parameter(description = "Mês de referência (formato yyyy-MM). Opcional. Padrão: mês atual.")
            @RequestParam(required = false) String referenceMonth) {
        FinancialDashboardResponse response = financialDashboardService.getDashboard(referenceMonth);
        return ResponseEntity.ok(response);
    }
}
