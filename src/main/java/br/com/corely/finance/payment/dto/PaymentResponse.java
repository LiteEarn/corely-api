package br.com.corely.finance.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de um pagamento (baixa manual) (EPIC-03-S06).
 */
public class PaymentResponse {

    private UUID id;
    private UUID receivableId;
    private UUID installmentId;
    private UUID studentId;
    private String studentName;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private PaymentMethodDto paymentMethod;
    private String externalReference;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentResponse() {
    }

    public PaymentResponse(UUID id, UUID receivableId, UUID installmentId, UUID studentId,
                           String studentName, LocalDate paymentDate, BigDecimal amount,
                           PaymentMethodDto paymentMethod, String externalReference, String notes,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.receivableId = receivableId;
        this.installmentId = installmentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.externalReference = externalReference;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getReceivableId() { return receivableId; }
    public UUID getInstallmentId() { return installmentId; }
    public UUID getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethodDto getPaymentMethod() { return paymentMethod; }
    public String getExternalReference() { return externalReference; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}