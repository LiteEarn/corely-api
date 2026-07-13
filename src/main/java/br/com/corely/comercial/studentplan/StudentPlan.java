package br.com.corely.comercial.studentplan;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.comercial.plan.Plan;
import br.com.corely.student.Student;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "comercial_student_plans")
public class StudentPlan extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentPlanStatus status = StudentPlanStatus.ACTIVE;

    @Column(name = "snapshot_name", nullable = false)
    private String snapshotName;

    @Column(name = "snapshot_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal snapshotValue;

    @Column(name = "snapshot_duration", nullable = false)
    private Integer snapshotDuration;

    @Column(name = "snapshot_rules", columnDefinition = "TEXT")
    private String snapshotRules;

    protected StudentPlan() {}

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public StudentPlanStatus getStatus() { return status; }
    public void setStatus(StudentPlanStatus status) { this.status = status; }
    public String getSnapshotName() { return snapshotName; }
    public void setSnapshotName(String snapshotName) { this.snapshotName = snapshotName; }
    public BigDecimal getSnapshotValue() { return snapshotValue; }
    public void setSnapshotValue(BigDecimal snapshotValue) { this.snapshotValue = snapshotValue; }
    public Integer getSnapshotDuration() { return snapshotDuration; }
    public void setSnapshotDuration(Integer snapshotDuration) { this.snapshotDuration = snapshotDuration; }
    public String getSnapshotRules() { return snapshotRules; }
    public void setSnapshotRules(String snapshotRules) { this.snapshotRules = snapshotRules; }
}
