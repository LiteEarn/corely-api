package br.com.corely.comercial.studentplan.dto;

import br.com.corely.comercial.studentplan.StudentPlanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class StudentPlanResponse {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private UUID planId;
    private String planName;
    private LocalDate startDate;
    private LocalDate endDate;
    private StudentPlanStatus status;
    private String snapshotName;
    private BigDecimal snapshotValue;
    private Integer snapshotDuration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StudentPlanResponse() {}

    public StudentPlanResponse(UUID id, UUID studentId, String studentName, UUID planId,
                               String planName, LocalDate startDate, LocalDate endDate,
                               StudentPlanStatus status, String snapshotName,
                               BigDecimal snapshotValue, Integer snapshotDuration,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.planId = planId;
        this.planName = planName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.snapshotName = snapshotName;
        this.snapshotValue = snapshotValue;
        this.snapshotDuration = snapshotDuration;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public UUID getPlanId() { return planId; }
    public String getPlanName() { return planName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public StudentPlanStatus getStatus() { return status; }
    public String getSnapshotName() { return snapshotName; }
    public BigDecimal getSnapshotValue() { return snapshotValue; }
    public Integer getSnapshotDuration() { return snapshotDuration; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
