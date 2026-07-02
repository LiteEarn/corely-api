package br.com.corely.dashboard.operational.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class OccupancyResponse {
    private UUID classGroupId;
    private String classGroupName;
    private int capacity;
    private long enrolled;
    private int occupancyPercentage;
}
