package br.com.corely.finance.installment.dto;

import br.com.corely.finance.installment.InstallmentStatus;
import br.com.corely.finance.situation.Situation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de uma parcela de recebível (EPIC-03-S02/S03).
 *
 * <p>Inclui a situação financeira calculada (EPIC-03-S03): em aberto, paga,
 * vencida ou estornada — derivada do status e do vencimento.</p>
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
    private Situation situation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InstallmentResponse() {
    }

    public InstallmentResponse(UUID id, UUID receivableId, UUID studentPlanId, UUID studentId,
                               String studentName, Integer installmentNumber, BigDecimal amount,
                               LocalDate dueDate, InstallmentStatus status, Situation situation,
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
        this.situation = situation;
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
    public Situation getSituation() { return situation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
