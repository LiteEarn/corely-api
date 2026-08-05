package br.com.corely.finance.receivable.dto;

import br.com.corely.finance.receivable.ReceivableStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de recebível (EPIC-03-S01).
 */
public class ReceivableResponse {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private ReceivableStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReceivableResponse() {
    }

    public ReceivableResponse(UUID id, UUID studentId, String studentName, String description,
                              BigDecimal amount, LocalDate dueDate, ReceivableStatus status,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.description = description;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public ReceivableStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
