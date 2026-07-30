package br.com.corely.financialdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Evolução da inadimplência por mês")
public class DelinquencyEvolutionItem {

    @Schema(description = "Mês de referência (yyyy-MM)", example = "2025-08")
    private String month;

    @Schema(description = "Quantidade de faturas vencidas no mês", example = "3")
    private Long overdueCount;

    @Schema(description = "Total de faturas no mês", example = "50")
    private Long totalCount;

    @Schema(description = "Taxa de inadimplência do mês (overdueCount / totalCount)", example = "0.06")
    private BigDecimal delinquencyRate;
}
