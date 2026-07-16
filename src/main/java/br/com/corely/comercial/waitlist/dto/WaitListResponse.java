package br.com.corely.comercial.waitlist.dto;

import br.com.corely.comercial.waitlist.WaitListStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class WaitListResponse {

    private UUID id;
    private UUID classSessionId;
    private UUID studentId;
    private Integer position;
    private WaitListStatus status;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WaitListResponse() {}

    public WaitListResponse(UUID id, UUID classSessionId, UUID studentId, Integer position,
                           WaitListStatus status, Boolean active, LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
        this.id = id;
        this.classSessionId = classSessionId;
        this.studentId = studentId;
        this.position = position;
        this.status = status;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getClassSessionId() { return classSessionId; }
    public UUID getStudentId() { return studentId; }
    public Integer getPosition() { return position; }
    public WaitListStatus getStatus() { return status; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
