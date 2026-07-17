package br.com.corely.finance.invoice.dto;

import br.com.corely.finance.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class InvoiceResponse {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private LocalDate dueDate;
    private BigDecimal amount;
    private String description;
    private InvoiceStatus status;
    private LocalDate paymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InvoiceResponse() {}

    public InvoiceResponse(UUID id, UUID studentId, String studentName, LocalDate dueDate,
                           BigDecimal amount, String description, InvoiceStatus status,
                           LocalDate paymentDate, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.dueDate = dueDate;
        this.amount = amount;
        this.description = description;
        this.status = status;
        this.paymentDate = paymentDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public InvoiceStatus getStatus() { return status; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
