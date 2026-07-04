package br.com.corely.dashboard.operational.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class DashboardAlertResponse {
    private String title;
    private String message;
    private AlertSeverity severity;
    private AlertType type;
    private String actionLabel;
    private String actionRoute;
    private UUID actionId;
}
