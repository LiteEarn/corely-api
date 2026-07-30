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
@Schema(description = "Métricas financeiras por plano. " +
        "Cada item representa um plano (contractSnapshot) identificado pelo seu ID único. " +
        "A receita e a contagem de faturas são baseadas exclusivamente em faturas com status PAID no mês de referência. " +
        "O ticket médio é calculado como receita / quantidade de faturas pagas, utilizando a mesma base amostral.")
public class PlanRevenueItem {

    @Schema(description = "Nome do plano", example = "Premium")
    private String planName;

    @Schema(description = "Preço base do plano registrado no contrato no momento da assinatura", example = "299.00")
    private BigDecimal planPrice;

    @Schema(description = "Receita total de faturas PAID no mês para este plano", example = "11960.00")
    private BigDecimal revenue;

    @Schema(description = "Quantidade de faturas PAID no mês para este plano (base de cálculo do ticket médio)", example = "40")
    private Long paidInvoiceCount;

    @Schema(description = "Quantidade de contratos ACTIVE neste plano (independente da receita do mês)", example = "45")
    private Long studentCount;

    @Schema(description = "Ticket médio por fatura paga: receita / paidInvoiceCount. " +
            "Utiliza a mesma base amostral da receita (faturas PAID) para evitar distorções.", example = "299.00")
    private BigDecimal averageTicket;
}
