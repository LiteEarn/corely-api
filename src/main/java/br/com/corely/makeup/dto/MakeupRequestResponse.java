package br.com.corely.makeup.dto;

import br.com.corely.makeup.MakeupRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MakeupRequestResponse(
        UUID id,
        UUID attendanceId,
        UUID targetSessionId,
        MakeupRequestStatus status,
        String reason,
        String studentName,
        String className,
        LocalDate absenceDate,
        String instructorName,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
