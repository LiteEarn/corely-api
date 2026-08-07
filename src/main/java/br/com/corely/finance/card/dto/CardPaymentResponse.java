package br.com.corely.finance.card.dto;

import br.com.corely.finance.card.CardPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de uma transação de cartão (EPIC-03-S08).
 */
public class CardPaymentResponse {

    private UUID id;
    private UUID receivableId;
    private UUID studentId;
    private String studentName;
    private String transactionId;
    private String cardBrand;
    private String lastFourDigits;
    private Integer installments;
    private BigDecimal amount;
    private CardPaymentStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public CardPaymentResponse() {
    }

    public CardPaymentResponse(UUID id, UUID receivableId, UUID studentId, String studentName,
                               String transactionId, String cardBrand, String lastFourDigits,
                               Integer installments, BigDecimal amount, CardPaymentStatus status,
                               LocalDateTime expiresAt, LocalDateTime paidAt, LocalDateTime createdAt) {
        this.id = id;
        this.receivableId = receivableId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.transactionId = transactionId;
        this.cardBrand = cardBrand;
        this.lastFourDigits = lastFourDigits;
        this.installments = installments;
        this.amount = amount;
        this.status = status;
        this.expiresAt = expiresAt;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getReceivableId() { return receivableId; }
    public UUID getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getTransactionId() { return transactionId; }
    public String getCardBrand() { return cardBrand; }
    public String getLastFourDigits() { return lastFourDigits; }
    public Integer getInstallments() { return installments; }
    public BigDecimal getAmount() { return amount; }
    public CardPaymentStatus getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}