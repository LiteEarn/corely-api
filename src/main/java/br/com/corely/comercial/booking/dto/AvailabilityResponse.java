package br.com.corely.comercial.booking.dto;

import java.time.LocalTime;
import java.util.UUID;

public class AvailabilityResponse {

    private UUID classSessionId;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private Integer bookedCount;
    private Integer availableSpots;
    private boolean available;
    private String reason;

    public AvailabilityResponse() {}

    public AvailabilityResponse(UUID classSessionId, LocalTime startTime, LocalTime endTime,
                                Integer capacity, Integer bookedCount, Integer availableSpots,
                                boolean available, String reason) {
        this.classSessionId = classSessionId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.bookedCount = bookedCount;
        this.availableSpots = availableSpots;
        this.available = available;
        this.reason = reason;
    }

    public UUID getClassSessionId() { return classSessionId; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Integer getCapacity() { return capacity; }
    public Integer getBookedCount() { return bookedCount; }
    public Integer getAvailableSpots() { return availableSpots; }
    public boolean isAvailable() { return available; }
    public String getReason() { return reason; }
}
