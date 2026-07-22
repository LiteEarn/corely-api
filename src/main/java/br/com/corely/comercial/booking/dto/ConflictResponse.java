package br.com.corely.comercial.booking.dto;

import java.util.UUID;

public class ConflictResponse {

    public enum ConflictType {
        INSTRUCTOR,
        ROOM,
        STUDENT
    }

    private ConflictType conflictType;
    private UUID conflictingBookingId;
    private String description;

    public ConflictResponse() {}

    public ConflictResponse(ConflictType conflictType, UUID conflictingBookingId, String description) {
        this.conflictType = conflictType;
        this.conflictingBookingId = conflictingBookingId;
        this.description = description;
    }

    public ConflictType getConflictType() { return conflictType; }
    public UUID getConflictingBookingId() { return conflictingBookingId; }
    public String getDescription() { return description; }
}
