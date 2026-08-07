package br.com.corely.finance.refund.dto;

import br.com.corely.finance.payment.dto.PaymentMethodDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de um estorno de pagamento (EPIC-03-S10).
 *
 * <p>Espelha o pagamento estornado acrescido da data de estorno e do motivo
 * informado.</p>
 */
public class RefundResponse {

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
    private String reason;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RefundResponse() {
    }

    public RefundResponse(UUID id, UUID receivableId, UUID installmentId, UUID studentId,
                          String studentName, LocalDate paymentDate, BigDecimal amount,
                          PaymentMethodDto paymentMethod, String externalReference, String notes,
                          String reason, LocalDateTime refundedAt, LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
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
        this.reason = reason;
        this.refundedAt = refundedAt;
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
    public String getReason() { return reason; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
