package br.com.corely.comercial.invoice.dto;

import br.com.corely.comercial.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class InvoiceResponse {

    private UUID id;
    private UUID studentPlanId;
    private String studentName;
    private String planName;
    private LocalDate dueDate;
    private String referenceMonth;
    private BigDecimal amount;
    private InvoiceStatus status;
    private LocalDate issueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InvoiceResponse() {}

    public InvoiceResponse(UUID id, UUID studentPlanId, String studentName, String planName,
                           LocalDate dueDate, String referenceMonth, BigDecimal amount,
                           InvoiceStatus status, LocalDate issueDate,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studentPlanId = studentPlanId;
        this.studentName = studentName;
        this.planName = planName;
        this.dueDate = dueDate;
        this.referenceMonth = referenceMonth;
        this.amount = amount;
        this.status = status;
        this.issueDate = issueDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getStudentPlanId() { return studentPlanId; }
    public String getStudentName() { return studentName; }
    public String getPlanName() { return planName; }
    public LocalDate getDueDate() { return dueDate; }
    public String getReferenceMonth() { return referenceMonth; }
    public BigDecimal getAmount() { return amount; }
    public InvoiceStatus getStatus() { return status; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
