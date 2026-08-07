package br.com.corely.finance.cashflow.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Requisição de registro de movimento de caixa (EPIC-03-S11).
 *
 * <p>Registra uma entrada (ou saída) no fluxo de caixa do estúdio corrente.
 * Entradas originadas de pagamentos podem referenciar o pagamento via
 * {@code paymentId} (validação de existência no tenant).</p>
 */
public class CashFlowEntryRequest {

    @NotNull
    private CashFlowEntryTypeDto entryType;

    @NotNull
    @FutureOrPresent
    private LocalDate entryDate;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Size(max = 500)
    private String description;

    @NotNull
    private CashFlowEntrySourceDto source;

    private UUID paymentId;

    @Size(max = 50)
    private String category;

    public CashFlowEntryRequest() {
    }

    public CashFlowEntryTypeDto getEntryType() {
        return entryType;
    }

    public void setEntryType(CashFlowEntryTypeDto entryType) {
        this.entryType = entryType;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CashFlowEntrySourceDto getSource() {
        return source;
    }

    public void setSource(CashFlowEntrySourceDto source) {
        this.source = source;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
