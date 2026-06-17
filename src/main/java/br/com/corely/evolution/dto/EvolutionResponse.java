package br.com.corely.evolution.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvolutionResponse {

    private UUID id;
    private UUID studioId;
    private UUID studentId;
    private String studentName;
    private UUID objectiveId;
    private String objectiveTitle;
    private UUID evaluationId;
    private LocalDate evolutionDate;
    private String title;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
