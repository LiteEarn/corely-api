package br.com.corely.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentEvaluationDTO {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private LocalDate evaluationDate;
    private BigDecimal weight;
    private BigDecimal height;
    private String observations;
    private LocalDateTime createdAt;
}
