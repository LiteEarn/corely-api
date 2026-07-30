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
@Schema(description = "Métricas financeiras por plano")
public class PlanRevenueItem {

    @Schema(description = "Nome do plano", example = "Premium")
    private String planName;

    @Schema(description = "Preço base do plano no contrato", example = "299.00")
    private BigDecimal planPrice;

    @Schema(description = "Receita total paga no mês para este plano", example = "8000.00")
    private BigDecimal revenue;

    @Schema(description = "Quantidade de alunos com contrato ativo neste plano", example = "40")
    private Long studentCount;

    @Schema(description = "Ticket médio por aluno neste plano (receita / quantidade)", example = "200.00")
    private BigDecimal averageTicket;
}
