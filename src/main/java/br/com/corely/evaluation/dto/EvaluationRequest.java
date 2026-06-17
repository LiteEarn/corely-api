package br.com.corely.evaluation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRequest {

    @NotNull(message = "Studio ID is required")
    private UUID studioId;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Evaluation date is required")
    private LocalDate evaluationDate;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.01", message = "Weight must be greater than zero")
    private BigDecimal weight;

    @NotNull(message = "Height is required")
    @DecimalMin(value = "0.01", message = "Height must be greater than zero")
    private BigDecimal height;

    @Size(max = 1000, message = "Observations must not exceed 1000 characters")
    private String observations;
}
