package br.com.corely.finance.cashflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de um movimento de caixa (EPIC-03-S11).
 */
public class CashFlowEntryResponse {

    private UUID id;
    private CashFlowEntryTypeDto entryType;
    private LocalDate entryDate;
    private BigDecimal amount;
    private String description;
    private CashFlowEntrySourceDto source;
    private UUID paymentId;
    private String category;
    private LocalDateTime createdAt;

    public CashFlowEntryResponse() {
    }

    public CashFlowEntryResponse(UUID id, CashFlowEntryTypeDto entryType, LocalDate entryDate,
                                 BigDecimal amount, String description, CashFlowEntrySourceDto source,
                                 UUID paymentId, String category, LocalDateTime createdAt) {
        this.id = id;
        this.entryType = entryType;
        this.entryDate = entryDate;
        this.amount = amount;
        this.description = description;
        this.source = source;
        this.paymentId = paymentId;
        this.category = category;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public CashFlowEntryTypeDto getEntryType() { return entryType; }
    public LocalDate getEntryDate() { return entryDate; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public CashFlowEntrySourceDto getSource() { return source; }
    public UUID getPaymentId() { return paymentId; }
    public String getCategory() { return category; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
