package br.com.corely.comercial.booking.dto;

import br.com.corely.comercial.booking.BookingStatus;
import br.com.corely.comercial.booking.CancelReason;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class BookingResponse {

    private UUID id;
    private UUID classSessionId;
    private UUID studentId;
    private String studentName;
    private LocalDateTime bookingDateTime;
    private BookingStatus status;
    private Boolean active;
    private CancelReason cancelReason;
    private String cancelDescription;
    private UUID cancelledBy;
    private LocalDateTime cancelledAt;
    private LocalDate classSessionDate;
    private LocalTime classSessionStartTime;
    private LocalTime classSessionEndTime;
    private UUID instructorId;
    private String instructorName;
    private Long roomId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BookingResponse() {}

    public BookingResponse(UUID id, UUID classSessionId, UUID studentId, String studentName,
                           LocalDateTime bookingDateTime, BookingStatus status, Boolean active,
                           CancelReason cancelReason, String cancelDescription, UUID cancelledBy,
                           LocalDateTime cancelledAt, LocalDate classSessionDate,
                           LocalTime classSessionStartTime, LocalTime classSessionEndTime,
                           UUID instructorId, String instructorName, Long roomId,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.classSessionId = classSessionId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.bookingDateTime = bookingDateTime;
        this.status = status;
        this.active = active;
        this.cancelReason = cancelReason;
        this.cancelDescription = cancelDescription;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
        this.classSessionDate = classSessionDate;
        this.classSessionStartTime = classSessionStartTime;
        this.classSessionEndTime = classSessionEndTime;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.roomId = roomId;
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
    public CancelReason getCancelReason() { return cancelReason; }
    public String getCancelDescription() { return cancelDescription; }
    public UUID getCancelledBy() { return cancelledBy; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public LocalDate getClassSessionDate() { return classSessionDate; }
    public LocalTime getClassSessionStartTime() { return classSessionStartTime; }
    public LocalTime getClassSessionEndTime() { return classSessionEndTime; }
    public UUID getInstructorId() { return instructorId; }
    public String getInstructorName() { return instructorName; }
    public Long getRoomId() { return roomId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
