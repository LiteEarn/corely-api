package br.com.corely.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {

    private UUID id;
    private UUID studioId;
    private UUID studentId;
    private String studentName;
    private LocalDate evaluationDate;
    private BigDecimal weight;
    private BigDecimal height;
    private String observations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
