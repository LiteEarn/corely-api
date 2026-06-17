package br.com.corely.evolution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvolutionRequest {

    @NotNull(message = "Studio ID is required")
    private UUID studioId;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    private UUID objectiveId;

    private UUID evaluationId;

    @NotNull(message = "Evolution date is required")
    private LocalDate evolutionDate;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @Size(max = 150, message = "Created by must not exceed 150 characters")
    private String createdBy;
}
