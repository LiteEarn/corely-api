package br.com.corely.financialdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Recebimentos de um dia específico")
public class PaymentDayResponse {

    @Schema(description = "Data do recebimento", example = "2026-07-15")
    private LocalDate date;

    @Schema(description = "Quantidade de recebimentos no dia", example = "5")
    private Long count;

    @Schema(description = "Valor total recebido no dia", example = "1250.00")
    private BigDecimal amount;
}
