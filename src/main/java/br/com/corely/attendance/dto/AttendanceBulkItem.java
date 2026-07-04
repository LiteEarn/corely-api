package br.com.corely.attendance.dto;

import br.com.corely.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AttendanceBulkItem {

    @NotNull(message = "Session ID é obrigatório")
    private UUID sessionId;

    private UUID enrollmentId;

    private UUID studentId;

    private AttendanceStatus status;

    private Boolean present;

    private String observation;

    private String notes;
}
