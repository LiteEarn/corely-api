package br.com.corely.finance.pix;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.finance.receivable.Receivable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cobrança Pix de um recebível (EPIC-03-S07).
 *
 * <p>Representa uma cobrança Pix gerada para um recebível em aberto, com
 * identificador de transação ({@code txid}), código copia-e-cola
 * ({@code copyPaste}), valor, validade e situação. Ao ser confirmada
 * (conciliação), liquida o recebível e registra o pagamento.</p>
 */
@Entity(name = "FinancePixPayment")
@Table(name = "pix_payments")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class PixPayment extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id", nullable = false, unique = true)
    private Receivable receivable;

    @Column(name = "txid", nullable = false, length = 64, unique = true)
    private String txid;

    @Column(name = "copy_paste", nullable = false, length = 512)
    private String copyPaste;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PixPaymentStatus status = PixPaymentStatus.PENDING;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public PixPayment() {
    }

    public Receivable getReceivable() {
        return receivable;
    }

    public void setReceivable(Receivable receivable) {
        this.receivable = receivable;
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public String getCopyPaste() {
        return copyPaste;
    }

    public void setCopyPaste(String copyPaste) {
        this.copyPaste = copyPaste;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PixPaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PixPaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
