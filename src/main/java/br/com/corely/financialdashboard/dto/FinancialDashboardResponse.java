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
@Schema(description = "Dashboard financeiro completo com visão consolidada do estúdio")
public class FinancialDashboardResponse {

    @Schema(description = "Resumo de receitas: valores faturados, recebidos, pendentes, vencidos e cancelados")
    private RevenueSummaryResponse revenueSummary;

    @Schema(description = "Métricas de alunos: ativos, inadimplentes, novos e cancelamentos no mês")
    private StudentMetricsResponse studentMetrics;

    @Schema(description = "Métricas por plano: faturamento, ticket médio e alunos ativos por plano")
    private PlanMetricsResponse planMetrics;

    @Schema(description = "Recebimentos: agrupados por dia e por mês (regime de caixa)")
    private PaymentMetricsResponse paymentMetrics;

    @Schema(description = "Indicadores financeiros: ticket médio, inadimplência e percentuais por status (regime de competência)")
    private FinancialIndicatorsResponse financialIndicators;

    @Schema(description = "Tendências: faturamento mensal, taxa de crescimento e evolução da inadimplência (12 meses)")
    private RevenueTrendResponse revenueTrend;
}
