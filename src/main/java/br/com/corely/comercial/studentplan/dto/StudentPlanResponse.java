package br.com.corely.comercial.studentplan.dto;

import br.com.corely.comercial.studentplan.StudentPlanStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class StudentPlanResponse {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private UUID contractSnapshotId;
    private String snapshotName;
    private LocalDate startDate;
    private LocalDate endDate;
    private StudentPlanStatus status;
    private LocalDate cancellationDate;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StudentPlanResponse() {}

    public StudentPlanResponse(UUID id, UUID studentId, String studentName, UUID contractSnapshotId,
                               String snapshotName, LocalDate startDate, LocalDate endDate,
                               StudentPlanStatus status, LocalDate cancellationDate,
                               String cancellationReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.contractSnapshotId = contractSnapshotId;
        this.snapshotName = snapshotName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.cancellationDate = cancellationDate;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public UUID getContractSnapshotId() { return contractSnapshotId; }
    public String getSnapshotName() { return snapshotName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public StudentPlanStatus getStatus() { return status; }
    public LocalDate getCancellationDate() { return cancellationDate; }
    public String getCancellationReason() { return cancellationReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
