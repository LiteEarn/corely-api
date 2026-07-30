package br.com.corely.financialdashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Métricas de recebimentos")
public class PaymentMetricsResponse {

    @Schema(description = "Recebimentos agrupados por dia no mês atual (últimos 7 dias com pagamento)")
    private List<PaymentDayResponse> byDay;

    @Schema(description = "Recebimentos agrupados por mês (últimos 12 meses)")
    private List<PaymentMonthResponse> byMonth;
}
