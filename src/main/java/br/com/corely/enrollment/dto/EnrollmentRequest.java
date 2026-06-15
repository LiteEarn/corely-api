package br.com.corely.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequest {

    @NotNull(message = "Studio ID is required")
    private UUID studioId;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Class group ID is required")
    private UUID classGroupId;

    private LocalDate enrollmentDate;

    private Boolean active;
}
