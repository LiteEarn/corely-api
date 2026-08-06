package br.com.corely.finance.installment.dto;

import br.com.corely.finance.installment.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de uma parcela de recebível (EPIC-03-S02).
 */
public class InstallmentResponse {

    private UUID id;
    private UUID receivableId;
    private UUID studentPlanId;
    private UUID studentId;
    private String studentName;
    private Integer installmentNumber;
    private BigDecimal amount;
    private LocalDate dueDate;
    private InstallmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InstallmentResponse() {
    }

    public InstallmentResponse(UUID id, UUID receivableId, UUID studentPlanId, UUID studentId,
                               String studentName, Integer installmentNumber, BigDecimal amount,
                               LocalDate dueDate, InstallmentStatus status,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.receivableId = receivableId;
        this.studentPlanId = studentPlanId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getReceivableId() { return receivableId; }
    public UUID getStudentPlanId() { return studentPlanId; }
    public UUID getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public Integer getInstallmentNumber() { return installmentNumber; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public InstallmentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
