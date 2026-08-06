package br.com.corely.finance.pix.dto;

import br.com.corely.finance.pix.PixPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de uma cobrança Pix (EPIC-03-S07).
 */
public class PixPaymentResponse {

    private UUID id;
    private UUID receivableId;
    private UUID studentId;
    private String studentName;
    private String txid;
    private String copyPaste;
    private BigDecimal amount;
    private PixPaymentStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public PixPaymentResponse() {
    }

    public PixPaymentResponse(UUID id, UUID receivableId, UUID studentId, String studentName,
                              String txid, String copyPaste, BigDecimal amount, PixPaymentStatus status,
                              LocalDateTime expiresAt, LocalDateTime paidAt, LocalDateTime createdAt) {
        this.id = id;
        this.receivableId = receivableId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.txid = txid;
        this.copyPaste = copyPaste;
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
    public String getTxid() { return txid; }
    public String getCopyPaste() { return copyPaste; }
    public BigDecimal getAmount() { return amount; }
    public PixPaymentStatus getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}