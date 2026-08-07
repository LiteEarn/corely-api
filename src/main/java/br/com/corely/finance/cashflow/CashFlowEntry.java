package br.com.corely.finance.cashflow;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.finance.payment.Payment;
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

/**
 * Movimento de caixa — entrada ou saída (EPIC-03-S11).
 *
 * <p>Registra um lançamento no fluxo de caixa do estúdio: entrada
 * ({@link CashFlowEntryType#ENTRY}) ou saída ({@link CashFlowEntryType#OUTFLOW}),
 * com data, valor, descrição e origem. Entradas originadas de pagamentos podem
 * referenciar o {@link Payment} correspondente.</p>
 */
@Entity(name = "FinanceCashFlowEntry")
@Table(name = "cash_flow_entries")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class CashFlowEntry extends ComercialBaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private CashFlowEntryType entryType;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private CashFlowEntrySource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "category", length = 50)
    private String category;

    public CashFlowEntry() {
    }

    public CashFlowEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(CashFlowEntryType entryType) {
        this.entryType = entryType;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CashFlowEntrySource getSource() {
        return source;
    }

    public void setSource(CashFlowEntrySource source) {
        this.source = source;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
