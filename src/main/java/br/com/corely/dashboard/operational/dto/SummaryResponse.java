package br.com.corely.dashboard.operational.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SummaryResponse {
    private DashboardKpiResponse kpis;
    private Integer averageOccupancy;
    private Integer todayAttendanceRate;
}
