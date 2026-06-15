package br.com.corely.classsession.dto;

import br.com.corely.classsession.ClassSessionStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionRequest {

    @NotNull(message = "Studio ID is required")
    private UUID studioId;

    @NotNull(message = "Instructor ID is required")
    private UUID instructorId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Max students is required")
    @Positive(message = "Max students must be greater than 0")
    private Integer maxStudents;

    private ClassSessionStatus status;
}
