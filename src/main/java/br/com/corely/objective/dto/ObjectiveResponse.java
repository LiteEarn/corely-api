package br.com.corely.objective.dto;

import br.com.corely.objective.ObjectiveStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectiveResponse {

    private UUID id;
    private UUID studioId;
    private UUID studentId;
    private String title;
    private String description;
    private ObjectiveStatus status;
    private LocalDate startDate;
    private LocalDate targetDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
