package br.com.corely.financialdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tendência de receita nos últimos 12 meses")
public class RevenueTrendResponse {

    @Schema(description = "Receita mensal nos últimos 12 meses")
    private List<MonthlyRevenueItem> monthlyRevenue;

    @Schema(description = "Crescimento mensal da receita: (último mês - penúltimo mês) / penúltimo mês. " +
            "Os dados são ordenados cronologicamente antes do cálculo. " +
            "Retorna 0 quando há menos de 2 meses com receita ou quando o mês anterior tem receita zero.", example = "0.05")
    private BigDecimal monthlyGrowthRate;

    @Schema(description = "Evolução da inadimplência mensal nos últimos 12 meses")
    private List<DelinquencyEvolutionItem> delinquencyEvolution;
}
