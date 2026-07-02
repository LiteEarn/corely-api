package br.com.corely.dashboard.operational.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OperationalDashboardResponse {
    private SummaryResponse summary;
    private List<UpcomingSessionResponse> upcomingSessions;
    private List<PendingMakeupResponse> pendingMakeupRequests;
    private List<OccupancyResponse> occupancy;
    private List<DashboardAlertResponse> alerts;
}
