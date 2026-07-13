package br.com.corely.comercial.billingschedule.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class BillingScheduleResponse {

    private UUID id;
    private UUID studentPlanId;
    private String studentName;
    private String planName;
    private BillingFrequencyDto frequency;
    private Integer billingDay;
    private LocalDate nextBillingDate;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BillingScheduleResponse() {}

    public BillingScheduleResponse(UUID id, UUID studentPlanId, String studentName, String planName,
                                   BillingFrequencyDto frequency, Integer billingDay,
                                   LocalDate nextBillingDate, Boolean active,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studentPlanId = studentPlanId;
        this.studentName = studentName;
        this.planName = planName;
        this.frequency = frequency;
        this.billingDay = billingDay;
        this.nextBillingDate = nextBillingDate;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getStudentPlanId() { return studentPlanId; }
    public String getStudentName() { return studentName; }
    public String getPlanName() { return planName; }
    public BillingFrequencyDto getFrequency() { return frequency; }
    public Integer getBillingDay() { return billingDay; }
    public LocalDate getNextBillingDate() { return nextBillingDate; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
