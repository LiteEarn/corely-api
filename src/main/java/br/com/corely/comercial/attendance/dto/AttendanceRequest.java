package br.com.corely.comercial.attendance.dto;

import br.com.corely.comercial.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class AttendanceRequest {

    @NotNull
    private UUID bookingId;

    @NotNull
    private AttendanceStatus status;

    @Size(max = 500)
    private String notes;

    public AttendanceRequest() {}

    public AttendanceRequest(UUID bookingId, AttendanceStatus status, String notes) {
        this.bookingId = bookingId;
        this.status = status;
        this.notes = notes;
    }

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
