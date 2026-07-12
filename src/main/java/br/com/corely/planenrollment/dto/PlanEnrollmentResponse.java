package br.com.corely.planenrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanEnrollmentResponse {

    private UUID id;
    private UUID studioId;
    private UUID studentId;
    private String studentName;
    private UUID planId;
    private String planName;
    private java.math.BigDecimal planValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private PlanEnrollmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
