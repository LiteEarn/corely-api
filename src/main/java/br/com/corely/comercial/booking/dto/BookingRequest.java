package br.com.corely.comercial.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class BookingRequest {

    @NotNull
    private UUID classSessionId;

    @NotNull
    private UUID studentId;

    public BookingRequest() {}

    public UUID getClassSessionId() { return classSessionId; }
    public void setClassSessionId(UUID classSessionId) { this.classSessionId = classSessionId; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
}
