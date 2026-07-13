package br.com.corely.comercial.invoice;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.comercial.studentplan.StudentPlan;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "comercial_invoices")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class Invoice extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_plan_id", nullable = false)
    private StudentPlan studentPlan;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "reference_month", nullable = false, length = 7)
    private String referenceMonth;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    public Invoice() {}

    public StudentPlan getStudentPlan() { return studentPlan; }
    public void setStudentPlan(StudentPlan studentPlan) { this.studentPlan = studentPlan; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getReferenceMonth() { return referenceMonth; }
    public void setReferenceMonth(String referenceMonth) { this.referenceMonth = referenceMonth; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
}
