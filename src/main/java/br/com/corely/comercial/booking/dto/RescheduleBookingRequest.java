package br.com.corely.comercial.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class RescheduleBookingRequest {

    @NotNull
    private UUID newClassSessionId;

    public RescheduleBookingRequest() {}

    public RescheduleBookingRequest(UUID newClassSessionId) {
        this.newClassSessionId = newClassSessionId;
    }

    public UUID getNewClassSessionId() { return newClassSessionId; }
    public void setNewClassSessionId(UUID newClassSessionId) { this.newClassSessionId = newClassSessionId; }
}
