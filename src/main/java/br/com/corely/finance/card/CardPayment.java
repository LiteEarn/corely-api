package br.com.corely.finance.card;

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
 * Transação de cartão de um recebível (EPIC-03-S08).
 *
 * <p>Representa um pagamento via cartão de crédito/débito gerado para um
 * recebível em aberto, com identificador de transação, bandeira, últimos quatro
 * dígitos do cartão, número de parcelas, valor, validade e situação. Ao ser
 * confirmada (conciliação), liquida o recebível e registra o pagamento.</p>
 *
 * <p>Nunca armazena dados completos do cartão (número, CVV, validade) — apenas
 * a bandeira e os últimos quatro dígitos, em conformidade com a norma de
 * segurança da indústria de cartões (PCI DSS).</p>
 */
@Entity(name = "FinanceCardPayment")
@Table(name = "card_payments")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class CardPayment extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id", nullable = false, unique = true)
    private Receivable receivable;

    @Column(name = "transaction_id", nullable = false, length = 64, unique = true)
    private String transactionId;

    @Column(name = "card_brand", nullable = false, length = 32)
    private String cardBrand;

    @Column(name = "last_four_digits", nullable = false, length = 4)
    private String lastFourDigits;

    @Column(name = "installments", nullable = false)
    private Integer installments = 1;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardPaymentStatus status = CardPaymentStatus.PENDING;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public CardPayment() {
    }

    public Receivable getReceivable() {
        return receivable;
    }

    public void setReceivable(Receivable receivable) {
        this.receivable = receivable;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }

    public String getLastFourDigits() {
        return lastFourDigits;
    }

    public void setLastFourDigits(String lastFourDigits) {
        this.lastFourDigits = lastFourDigits;
    }

    public Integer getInstallments() {
        return installments;
    }

    public void setInstallments(Integer installments) {
        this.installments = installments;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public CardPaymentStatus getStatus() {
        return status;
    }

    public void setStatus(CardPaymentStatus status) {
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