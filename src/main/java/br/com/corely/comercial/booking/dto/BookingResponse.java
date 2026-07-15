package br.com.corely.comercial.booking.dto;

import br.com.corely.comercial.booking.BookingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class BookingResponse {

    private UUID id;
    private UUID classSessionId;
    private UUID studentId;
    private String studentName;
    private LocalDateTime bookingDateTime;
    private BookingStatus status;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BookingResponse() {}

    public BookingResponse(UUID id, UUID classSessionId, UUID studentId, String studentName,
                           LocalDateTime bookingDateTime, BookingStatus status, Boolean active,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.classSessionId = classSessionId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.bookingDateTime = bookingDateTime;
        this.status = status;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getClassSessionId() { return classSessionId; }
    public UUID getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public LocalDateTime getBookingDateTime() { return bookingDateTime; }
    public BookingStatus getStatus() { return status; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
