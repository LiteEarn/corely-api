package br.com.corely.finance.cash.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Requisição de pagamento em dinheiro (EPIC-03-S09).
 *
 * <p>Registra a liquidação de um recebível (ou de uma parcela específica) em
 * dinheiro. A forma de pagamento é sempre {@code CASH}; o valor deve ser
 * positivo e igual ao valor do recebível/parcela.</p>
 */
public class CashPaymentRequest {

    @NotNull
    private UUID receivableId;

    private UUID installmentId;

    @NotNull
    private LocalDate paymentDate;

    @NotNull
    @Positive
    private BigDecimal amount;

    @Size(max = 1000)
    private String notes;

    public CashPaymentRequest() {
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
