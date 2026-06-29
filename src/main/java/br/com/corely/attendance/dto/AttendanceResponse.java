package br.com.corely.attendance.dto;

import br.com.corely.attendance.AttendanceStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID classSessionId,
        UUID enrollmentId,
        String studentName,
        AttendanceStatus status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
