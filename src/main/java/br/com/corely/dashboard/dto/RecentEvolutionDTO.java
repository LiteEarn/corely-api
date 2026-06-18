package br.com.corely.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentEvolutionDTO {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private LocalDate evolutionDate;
    private String title;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
}
