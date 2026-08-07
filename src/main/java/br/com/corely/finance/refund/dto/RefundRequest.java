package br.com.corely.finance.refund.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Requisição de estorno de pagamento (EPIC-03-S10).
 *
 * <p>Estorna um pagamento registrado na baixa manual, devolvendo o recebível
 * (ou parcela) à situação de aberto e registrando a movimentação de estorno no
 * histórico.</p>
 */
public class RefundRequest {

    @NotNull
    private UUID paymentId;

    @Size(max = 500)
    private String reason;

    public RefundRequest() {
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
