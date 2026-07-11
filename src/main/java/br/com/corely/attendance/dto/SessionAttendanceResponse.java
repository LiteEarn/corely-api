package br.com.corely.attendance.dto;

import br.com.corely.attendance.AttendanceStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionAttendanceResponse(
        UUID id,
        UUID classSessionId,
        UUID enrollmentId,
        UUID studentId,
        String studentName,
        AttendanceStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
