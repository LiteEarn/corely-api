package br.com.corely.planenrollment.dto;

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
public class StudentPlanResponse {

    private UUID id;
    private UUID planId;
    private String planName;
    private String planType;
    private BigDecimal planValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private PlanEnrollmentStatus status;
    private LocalDateTime createdAt;
}
