package br.com.corely.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private Long activeStudents;
    private Long activeInstructors;
    private Long activeClassGroups;
    private Long totalEnrollments;
    private Long attendanceThisWeek;
    private Long attendanceThisMonth;
    private Long activeObjectives;
    private Long completedObjectives;
    private Long evaluationsThisMonth;
    private Long evolutionsThisMonth;
    private BigDecimal occupancyRate;
    private List<RecentEvaluationDTO> recentEvaluations;
    private List<RecentEvolutionDTO> recentEvolutions;
}
