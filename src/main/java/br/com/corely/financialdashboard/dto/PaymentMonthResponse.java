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
@Schema(description = "Recebimentos de um mês específico")
public class PaymentMonthResponse {

    @Schema(description = "Mês de referência (yyyy-MM)", example = "2026-07")
    private String month;

    @Schema(description = "Quantidade de recebimentos no mês", example = "50")
    private Long count;

    @Schema(description = "Valor total recebido no mês", example = "12000.00")
    private BigDecimal amount;
}
