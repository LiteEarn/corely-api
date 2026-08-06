package br.com.corely.finance.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Requisição de baixa manual de pagamento (EPIC-03-S06).
 *
 * <p>Registra a liquidação de um recebível (ou de uma parcela específica) por
 * meio de pagamento manual. O valor deve ser positivo e igual ao valor do
 * recebível/parcela.</p>
 */
public class PaymentRequest {

    @NotNull
    private UUID receivableId;

    private UUID installmentId;

    @NotNull
    private LocalDate paymentDate;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private PaymentMethodDto paymentMethod;

    @Size(max = 255)
    private String externalReference;

    @Size(max = 1000)
    private String notes;

    public PaymentRequest() {
    }

    public UUID getReceivableId() {
        return receivableId;
    }

    public void setReceivableId(UUID receivableId) {
        this.receivableId = receivableId;
    }

    public UUID getInstallmentId() {
        return installmentId;
    }

    public void setInstallmentId(UUID installmentId) {
        this.installmentId = installmentId;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethodDto getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethodDto paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}