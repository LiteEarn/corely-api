package br.com.corely.comercial.waitlist.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class WaitListRequest {

    @NotNull
    private UUID classSessionId;

    @NotNull
    private UUID studentId;

    public WaitListRequest() {}

    public UUID getClassSessionId() { return classSessionId; }
    public void setClassSessionId(UUID classSessionId) { this.classSessionId = classSessionId; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
}
