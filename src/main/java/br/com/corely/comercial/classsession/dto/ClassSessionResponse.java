package br.com.corely.comercial.classsession.dto;

import br.com.corely.comercial.classsession.SessionCancelReason;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class ClassSessionResponse {

    private UUID id;
    private UUID scheduleSlotId;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private Integer bookedCount;
    private SessionStatusDto status;
    private Boolean active;
    private SessionCancelReason cancelReason;
    private String cancelDescription;
    private UUID cancelledBy;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ClassSessionResponse() {}

    public ClassSessionResponse(UUID id, UUID scheduleSlotId, LocalDate sessionDate,
                                LocalTime startTime, LocalTime endTime, Integer capacity,
                                Integer bookedCount, SessionStatusDto status, Boolean active,
                                SessionCancelReason cancelReason, String cancelDescription,
                                UUID cancelledBy, LocalDateTime cancelledAt,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.scheduleSlotId = scheduleSlotId;
        this.sessionDate = sessionDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.bookedCount = bookedCount;
        this.status = status;
        this.active = active;
        this.cancelReason = cancelReason;
        this.cancelDescription = cancelDescription;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getScheduleSlotId() { return scheduleSlotId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Integer getCapacity() { return capacity; }
    public Integer getBookedCount() { return bookedCount; }
    public SessionStatusDto getStatus() { return status; }
    public Boolean getActive() { return active; }
    public SessionCancelReason getCancelReason() { return cancelReason; }
    public String getCancelDescription() { return cancelDescription; }
    public UUID getCancelledBy() { return cancelledBy; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
