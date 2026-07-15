package br.com.corely.comercial.classsession;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.comercial.scheduleslot.ScheduleSlot;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity(name = "ComercialClassSession")
@Table(name = "comercial_class_sessions")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class ClassSession extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_slot_id", nullable = false)
    private ScheduleSlot scheduleSlot;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "booked_count", nullable = false)
    private Integer bookedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public ClassSession() {}

    public ScheduleSlot getScheduleSlot() { return scheduleSlot; }
    public void setScheduleSlot(ScheduleSlot scheduleSlot) { this.scheduleSlot = scheduleSlot; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getBookedCount() { return bookedCount; }
    public void setBookedCount(Integer bookedCount) { this.bookedCount = bookedCount; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
