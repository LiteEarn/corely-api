package br.com.corely.attendance.dto;

import br.com.corely.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {

    @NotNull(message = "Enrollment ID is required")
    private UUID enrollmentId;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
