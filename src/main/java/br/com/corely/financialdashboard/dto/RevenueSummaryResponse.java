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
@Schema(description = "Resumo financeiro do mês consultado")
public class RevenueSummaryResponse {

    @Schema(description = "Receita prevista do mês (total de faturas PENDING + PAID + OVERDUE)", example = "15000.00")
    private BigDecimal expectedRevenue;

    @Schema(description = "Receita recebida (faturas com status PAID)", example = "12000.00")
    private BigDecimal receivedRevenue;

    @Schema(description = "Receita pendente (faturas com status PENDING)", example = "2000.00")
    private BigDecimal pendingRevenue;

    @Schema(description = "Receita vencida (faturas com status OVERDUE)", example = "800.00")
    private BigDecimal overdueRevenue;

    @Schema(description = "Receita cancelada (faturas com status CANCELLED)", example = "200.00")
    private BigDecimal cancelledRevenue;

    @Schema(description = "Total faturado no mês (soma de todos os status)", example = "15000.00")
    private BigDecimal totalInvoiced;

    @Schema(description = "Quantidade total de faturas no mês", example = "75")
    private Long totalInvoiceCount;

    @Schema(description = "Quantidade de faturas pagas", example = "60")
    private Long paidInvoiceCount;

    @Schema(description = "Quantidade de faturas pendentes", example = "10")
    private Long pendingInvoiceCount;

    @Schema(description = "Quantidade de faturas vencidas", example = "4")
    private Long overdueInvoiceCount;

    @Schema(description = "Quantidade de faturas canceladas", example = "1")
    private Long cancelledInvoiceCount;
}
