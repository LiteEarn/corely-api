package br.com.corely.comercial.attendance;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.comercial.booking.Booking;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

@Entity(name = "ComercialAttendance")
@Table(name = "comercial_attendances",
       uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id"}))
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class Attendance extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public Attendance() {}

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
