package br.com.corely.finance.receivable.dto;

import br.com.corely.finance.receivable.ReceivableStatus;
import br.com.corely.finance.situation.Situation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de recebível (EPIC-03-S01/S03).
 *
 * <p>Inclui a situação financeira calculada (EPIC-03-S03): em aberto, paga,
 * vencida ou estornada — derivada do status e do vencimento.</p>
 */
public class ReceivableResponse {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private String description;
    private BigDecimal amount;
    private LocalDate dueDate;
    private ReceivableStatus status;
    private Situation situation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReceivableResponse() {
    }

    public ReceivableResponse(UUID id, UUID studentId, String studentName, String description,
                              BigDecimal amount, LocalDate dueDate, ReceivableStatus status,
                              Situation situation,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.description = description;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
        this.situation = situation;
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
    public Situation getSituation() { return situation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
