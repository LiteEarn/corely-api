package br.com.corely.finance.payment;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.finance.installment.ReceivableInstallment;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pagamento — baixa manual de um recebível (EPIC-03-S06).
 *
 * <p>Registra a liquidação de um recebível (ou de uma parcela específica) por
 * meio de uma baixa manual, com data, valor, forma de pagamento e observações.
 * Ao ser registrado, atualiza a situação do recebível/parcela para paga.</p>
 */
@Entity(name = "FinancePayment")
@Table(name = "payments")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class Payment extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_id")
    private ReceivableInstallment installment;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "external_reference", length = 255)
    private String externalReference;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    public Payment() {
    }

    public Receivable getReceivable() {
        return receivable;
    }

    public void setReceivable(Receivable receivable) {
        this.receivable = receivable;
    }

    public ReceivableInstallment getInstallment() {
        return installment;
    }

    public void setInstallment(ReceivableInstallment installment) {
        this.installment = installment;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }
}