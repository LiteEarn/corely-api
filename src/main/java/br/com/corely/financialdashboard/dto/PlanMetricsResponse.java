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
@Schema(description = "Métricas por plano")
public class PlanMetricsResponse {

    @Schema(description = "Lista de métricas por plano")
    private List<PlanRevenueItem> plans;

    @Schema(description = "Quantidade total de planos ativos", example = "5")
    private Long totalActivePlans;
}
