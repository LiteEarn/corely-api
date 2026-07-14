package br.com.corely.comercial.studentplan;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.student.Student;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;

@Entity
@Table(name = "comercial_student_plans")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class StudentPlan extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_snapshot_id", nullable = false)
    private ContractSnapshot contractSnapshot;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentPlanStatus status = StudentPlanStatus.ACTIVE;

    @Column(name = "cancellation_date")
    private LocalDate cancellationDate;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "booking_blocked", nullable = false)
    private Boolean bookingBlocked = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "suspension_reason", length = 20)
    private SuspensionReason suspensionReason;

    public StudentPlan() {}

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public ContractSnapshot getContractSnapshot() { return contractSnapshot; }
    public void setContractSnapshot(ContractSnapshot contractSnapshot) { this.contractSnapshot = contractSnapshot; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public StudentPlanStatus getStatus() { return status; }
    public void setStatus(StudentPlanStatus status) { this.status = status; }
    public LocalDate getCancellationDate() { return cancellationDate; }
    public void setCancellationDate(LocalDate cancellationDate) { this.cancellationDate = cancellationDate; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public Boolean getBookingBlocked() { return bookingBlocked; }
    public void setBookingBlocked(Boolean bookingBlocked) { this.bookingBlocked = bookingBlocked; }
    public SuspensionReason getSuspensionReason() { return suspensionReason; }
    public void setSuspensionReason(SuspensionReason suspensionReason) { this.suspensionReason = suspensionReason; }
}
