package br.com.corely.comercial.makeup.dto;

import br.com.corely.comercial.makeup.MakeUpCreditStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class MakeUpCreditResponse {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private UUID originalAttendanceId;
    private UUID originalClassSessionId;
    private UUID makeUpBookingId;
    private LocalDate expirationDate;
    private MakeUpCreditStatus status;
    private String reason;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MakeUpCreditResponse() {}

    public MakeUpCreditResponse(UUID id, UUID studentId, String studentName, UUID originalAttendanceId,
                                UUID originalClassSessionId, UUID makeUpBookingId, LocalDate expirationDate,
                                MakeUpCreditStatus status, String reason, Boolean active,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.originalAttendanceId = originalAttendanceId;
        this.originalClassSessionId = originalClassSessionId;
        this.makeUpBookingId = makeUpBookingId;
        this.expirationDate = expirationDate;
        this.status = status;
        this.reason = reason;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public UUID getOriginalAttendanceId() { return originalAttendanceId; }
    public UUID getOriginalClassSessionId() { return originalClassSessionId; }
    public UUID getMakeUpBookingId() { return makeUpBookingId; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public MakeUpCreditStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
