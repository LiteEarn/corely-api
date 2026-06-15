package br.com.corely.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceItemRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Present is required")
    private Boolean present;

    private String notes;
}
