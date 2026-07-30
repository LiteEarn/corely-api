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
@Schema(description = "Evolução da inadimplência por mês. " +
        "A taxa de inadimplência é calculada como overdueCount / totalInvoices, " +
        "onde totalInvoices representa TODAS as faturas emitidas no mês " +
        "(independente do status: PAID, PENDING, OVERDUE, CANCELLED). " +
        "Isso garante que o denominador seja a base total de faturas, " +
        "e não apenas as pagas, evitando distorção do indicador.")
public class DelinquencyEvolutionItem {

    @Schema(description = "Mês de referência (yyyy-MM)", example = "2025-08")
    private String month;

    @Schema(description = "Quantidade de faturas com status OVERDUE no mês", example = "3")
    private Long overdueCount;

    @Schema(description = "Total de faturas emitidas no mês (todos os status: PAID, PENDING, OVERDUE, CANCELLED)", example = "50")
    private Long totalCount;

    @Schema(description = "Taxa de inadimplência = overdueCount / totalInvoices. " +
            "Exemplo: 3 faturas vencidas / 40 faturas emitidas = 0.075 (7,5%)", example = "0.06")
    private BigDecimal delinquencyRate;
}
