package br.com.corely.dashboard.operational.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class PendingMakeupResponse {
    private UUID id;
    private String studentName;
    private String className;
    private LocalDateTime requestedAt;
    private String reason;
}
