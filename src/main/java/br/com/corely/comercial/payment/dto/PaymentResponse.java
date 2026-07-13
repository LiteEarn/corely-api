package br.com.corely.comercial.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentResponse {

    private UUID id;
    private UUID invoiceId;
    private String studentName;
    private String planName;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private PaymentMethodDto paymentMethod;
    private String externalReference;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PaymentResponse() {}

    public PaymentResponse(UUID id, UUID invoiceId, String studentName, String planName,
                           LocalDate paymentDate, BigDecimal amount, PaymentMethodDto paymentMethod,
                           String externalReference, String notes,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.studentName = studentName;
        this.planName = planName;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.externalReference = externalReference;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getInvoiceId() { return invoiceId; }
    public String getStudentName() { return studentName; }
    public String getPlanName() { return planName; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethodDto getPaymentMethod() { return paymentMethod; }
    public String getExternalReference() { return externalReference; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
