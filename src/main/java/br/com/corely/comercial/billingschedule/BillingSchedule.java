package br.com.corely.comercial.billingschedule;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.comercial.studentplan.StudentPlan;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;

@Entity
@Table(name = "comercial_billing_schedules")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class BillingSchedule extends ComercialBaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_plan_id", nullable = false, unique = true)
    private StudentPlan studentPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private BillingFrequency frequency;

    @Column(name = "billing_day", nullable = false)
    private Integer billingDay;

    @Column(name = "next_billing_date", nullable = false)
    private LocalDate nextBillingDate;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public BillingSchedule() {}

    public StudentPlan getStudentPlan() { return studentPlan; }
    public void setStudentPlan(StudentPlan studentPlan) { this.studentPlan = studentPlan; }
    public BillingFrequency getFrequency() { return frequency; }
    public void setFrequency(BillingFrequency frequency) { this.frequency = frequency; }
    public Integer getBillingDay() { return billingDay; }
    public void setBillingDay(Integer billingDay) { this.billingDay = billingDay; }
    public LocalDate getNextBillingDate() { return nextBillingDate; }
    public void setNextBillingDate(LocalDate nextBillingDate) { this.nextBillingDate = nextBillingDate; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
