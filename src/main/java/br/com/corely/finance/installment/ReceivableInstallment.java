package br.com.corely.finance.installment;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.comercial.studentplan.StudentPlan;
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

/**
 * Parcela de um recebível gerada a partir da matrícula/plano (EPIC-03-S02).
 *
 * <p>Representa um desdobramento mensal de cobrança de um aluno matriculado em
 * um plano. Cada parcela possui número, valor, vencimento e situação própria.
 * A origem é a matrícula ({@link StudentPlan}) e o título associado é o
 * {@link Receivable}.</p>
 */
@Entity(name = "ReceivableInstallment")
@Table(name = "receivable_installments")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class ReceivableInstallment extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id", nullable = false)
    private Receivable receivable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_plan_id", nullable = false)
    private StudentPlan studentPlan;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InstallmentStatus status = InstallmentStatus.OPEN;

    public ReceivableInstallment() {
    }

    public Receivable getReceivable() {
        return receivable;
    }

    public void setReceivable(Receivable receivable) {
        this.receivable = receivable;
    }

    public StudentPlan getStudentPlan() {
        return studentPlan;
    }

    public void setStudentPlan(StudentPlan studentPlan) {
        this.studentPlan = studentPlan;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(Integer installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public InstallmentStatus getStatus() {
        return status;
    }

    public void setStatus(InstallmentStatus status) {
        this.status = status;
    }
}
