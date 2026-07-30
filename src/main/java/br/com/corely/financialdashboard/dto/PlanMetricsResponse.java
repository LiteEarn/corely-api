package br.com.corely.financialdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Métricas por plano. " +
        "Agrupa indicadores de receita e contratos ativos por plano (contractSnapshot). " +
        "Cada plano é identificado pelo contractSnapshot.id, garantindo estabilidade mesmo com alterações futuras no nome.")
public class PlanMetricsResponse {

    @Schema(description = "Lista de métricas por plano, ordenada pela ordem de criação dos planos")
    private List<PlanRevenueItem> plans;

    @Schema(description = "Quantidade total de planos ativos (com ao menos um contrato ACTIVE ou fatura PAID no mês)", example = "5")
    private Long totalActivePlans;
}
