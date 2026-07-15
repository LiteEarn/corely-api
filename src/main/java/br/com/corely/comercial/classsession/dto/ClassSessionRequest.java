package br.com.corely.comercial.classsession.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class ClassSessionRequest {

    @NotNull
    private UUID scheduleSlotId;

    @NotNull
    private LocalDate sessionDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    private SessionStatusDto status;
    private Boolean active;

    public ClassSessionRequest() {}

    public UUID getScheduleSlotId() { return scheduleSlotId; }
    public void setScheduleSlotId(UUID scheduleSlotId) { this.scheduleSlotId = scheduleSlotId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public SessionStatusDto getStatus() { return status; }
    public void setStatus(SessionStatusDto status) { this.status = status; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
