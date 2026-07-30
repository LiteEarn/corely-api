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
@Schema(description = "Indicadores financeiros consolidados. " +
        "Todos os percentuais utilizam regime de competência (accrual basis): " +
        "tanto numerador quanto denominador são extraídos da tabela de faturas (Invoice) " +
        "para o mesmo mês de referência, garantindo consistência.")
public class FinancialIndicatorsResponse {

    @Schema(description = "Ticket médio por fatura paga: soma das faturas PAID / quantidade de faturas PAID. " +
            "Indica o valor médio efetivamente recebido por fatura no mês. " +
            "Base: faturas PAID do mês de referência (regime de competência).", example = "240.00")
    private BigDecimal averageTicket;

    @Schema(description = "Taxa de inadimplência: faturas OVERDUE / total de faturas emitidas no mês. " +
            "Regime de competência: todas as faturas consideradas são do mesmo mês de referência.", example = "0.0667")
    private BigDecimal delinquencyRate;

    @Schema(description = "Percentual do faturamento que já foi recebido: " +
            "soma das faturas PAID / soma total das faturas emitidas. " +
            "Regime de competência: numerador e denominador são do mesmo mês de referência. " +
            "Não inclui pagamentos de meses anteriores recebidos neste período.", example = "0.80")
    private BigDecimal receivedPercentage;

    @Schema(description = "Percentual do faturamento pendente: soma das faturas PENDING / soma total das faturas emitidas", example = "0.13")
    private BigDecimal pendingPercentage;

    @Schema(description = "Percentual do faturamento vencido: soma das faturas OVERDUE / soma total das faturas emitidas", example = "0.05")
    private BigDecimal overduePercentage;

    @Schema(description = "Percentual do faturamento cancelado: soma das faturas CANCELLED / soma total das faturas emitidas", example = "0.02")
    private BigDecimal cancelledPercentage;
}
