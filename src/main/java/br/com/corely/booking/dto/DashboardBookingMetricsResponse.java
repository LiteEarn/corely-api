package br.com.corely.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardBookingMetricsResponse {

    private long todayClasses;
    private long weekClasses;
    private double occupancyRate;
    private double noShowRate;
    private double cancellationRate;
}
