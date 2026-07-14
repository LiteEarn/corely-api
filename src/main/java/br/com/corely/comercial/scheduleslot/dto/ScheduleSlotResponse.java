package br.com.corely.comercial.scheduleslot.dto;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class ScheduleSlotResponse {

    private UUID id;
    private UUID scheduleId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ScheduleSlotResponse() {}

    public ScheduleSlotResponse(UUID id, UUID scheduleId, DayOfWeek dayOfWeek,
                                LocalTime startTime, LocalTime endTime, Integer capacity,
                                Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.scheduleId = scheduleId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getScheduleId() { return scheduleId; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Integer getCapacity() { return capacity; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
