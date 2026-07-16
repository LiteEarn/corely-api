package br.com.corely.comercial.attendance.dto;

import br.com.corely.comercial.attendance.AttendanceStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class AttendanceResponse {

    private UUID id;
    private UUID classSessionId;
    private UUID bookingId;
    private UUID studentId;
    private String studentName;
    private AttendanceStatus status;
    private String notes;
    private LocalDateTime checkedInAt;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AttendanceResponse() {}

    public AttendanceResponse(UUID id, UUID classSessionId, UUID bookingId, UUID studentId,
                              String studentName, AttendanceStatus status, String notes,
                              LocalDateTime checkedInAt, Boolean active,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.classSessionId = classSessionId;
        this.bookingId = bookingId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.status = status;
        this.notes = notes;
        this.checkedInAt = checkedInAt;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getClassSessionId() { return classSessionId; }
    public UUID getBookingId() { return bookingId; }
    public UUID getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public AttendanceStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
