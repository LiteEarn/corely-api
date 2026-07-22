package br.com.corely.comercial.booking.dto;

import br.com.corely.comercial.booking.BlockType;

import java.time.LocalDateTime;
import java.util.UUID;

public class TimeBlockResponse {

    private UUID id;
    private UUID instructorId;
    private String instructorName;
    private Long roomId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String reason;
    private BlockType blockType;
    private Boolean active;

    public TimeBlockResponse() {}

    public TimeBlockResponse(UUID id, UUID instructorId, String instructorName, Long roomId,
                             LocalDateTime startDateTime, LocalDateTime endDateTime,
                             String reason, BlockType blockType, Boolean active) {
        this.id = id;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.roomId = roomId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.reason = reason;
        this.blockType = blockType;
        this.active = active;
    }

    public UUID getId() { return id; }
    public UUID getInstructorId() { return instructorId; }
    public String getInstructorName() { return instructorName; }
    public Long getRoomId() { return roomId; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public String getReason() { return reason; }
    public BlockType getBlockType() { return blockType; }
    public Boolean getActive() { return active; }
}
