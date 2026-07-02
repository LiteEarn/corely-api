package br.com.corely.dashboard.operational.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SummaryResponse {
    private long classesToday;
    private long classesInProgress;
    private long studentsPresentToday;
    private long pendingMakeupRequests;
}
