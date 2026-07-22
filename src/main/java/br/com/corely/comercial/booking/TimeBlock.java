package br.com.corely.comercial.booking;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.instructor.Instructor;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

@Entity(name = "ComercialTimeBlock")
@Table(name = "comercial_time_blocks")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class TimeBlock extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = true)
    private Instructor instructor;

    @Column(name = "room_id", nullable = true)
    private Long roomId;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    private BlockType blockType;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public TimeBlock() {}

    public Instructor getInstructor() { return instructor; }
    public void setInstructor(Instructor instructor) { this.instructor = instructor; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BlockType getBlockType() { return blockType; }
    public void setBlockType(BlockType blockType) { this.blockType = blockType; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
