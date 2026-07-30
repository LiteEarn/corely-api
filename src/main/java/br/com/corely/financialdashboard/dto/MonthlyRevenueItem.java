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
@Schema(description = "Receita de um mês na tendência")
public class MonthlyRevenueItem {

    @Schema(description = "Mês de referência (yyyy-MM)", example = "2025-08")
    private String month;

    @Schema(description = "Receita paga no mês", example = "10000.00")
    private BigDecimal revenue;

    @Schema(description = "Quantidade de faturas pagas no mês", example = "45")
    private Long invoiceCount;
}
