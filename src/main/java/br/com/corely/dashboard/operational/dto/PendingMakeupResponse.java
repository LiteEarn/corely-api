package br.com.corely.dashboard.operational.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
public class PendingMakeupResponse {
    private UUID id;
    private String studentName;
    private String className;
    private LocalDate absenceDate;
    private String reason;
}
