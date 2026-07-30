package br.com.corely.financialdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dashboard financeiro completo com visão consolidada do estudio")
public class FinancialDashboardResponse {

    @Schema(description = "Resumo financeiro do mês consultado")
    private RevenueSummaryResponse revenueSummary;

    @Schema(description = "Métricas de clientes")
    private StudentMetricsResponse studentMetrics;

    @Schema(description = "Métricas por plano")
    private PlanMetricsResponse planMetrics;

    @Schema(description = "Métricas de recebimentos")
    private PaymentMetricsResponse paymentMetrics;

    @Schema(description = "Indicadores financeiros consolidados")
    private FinancialIndicatorsResponse financialIndicators;

    @Schema(description = "Tendência de receita nos últimos 12 meses")
    private RevenueTrendResponse revenueTrend;
}
