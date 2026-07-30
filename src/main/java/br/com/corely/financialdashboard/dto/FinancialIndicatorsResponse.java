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
@Schema(description = "Indicadores financeiros consolidados")
public class FinancialIndicatorsResponse {

    @Schema(description = "Ticket médio (valor total recebido / quantidade de recebimentos no mês)", example = "240.00")
    private BigDecimal averageTicket;

    @Schema(description = "Taxa de inadimplência (faturas OVERDUE / total de faturas emitidas no mês)", example = "0.0667")
    private BigDecimal delinquencyRate;

    @Schema(description = "Percentual da receita recebida em relação ao total faturado", example = "0.80")
    private BigDecimal receivedPercentage;

    @Schema(description = "Percentual da receita pendente em relação ao total faturado", example = "0.13")
    private BigDecimal pendingPercentage;

    @Schema(description = "Percentual da receita vencida em relação ao total faturado", example = "0.05")
    private BigDecimal overduePercentage;

    @Schema(description = "Percentual da receita cancelada em relação ao total faturado", example = "0.02")
    private BigDecimal cancelledPercentage;
}
