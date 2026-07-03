package br.com.corely.dashboard.operational.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ClassOccupancyResponse {
    private UUID classGroupId;
    private String className;
    private int capacity;
    private long enrolled;
    private int occupancyPercent;
}
