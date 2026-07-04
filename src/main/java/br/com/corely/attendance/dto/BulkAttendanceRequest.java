package br.com.corely.attendance.dto;

import br.com.corely.attendance.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkAttendanceRequest {

    @NotEmpty(message = "At least one attendance entry is required")
    private @Valid List<AttendanceEntry> entries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceEntry {

        @NotNull(message = "Session ID is required")
        private UUID sessionId;

        @NotNull(message = "Enrollment ID is required")
        private UUID enrollmentId;

        @NotNull(message = "Status is required")
        private AttendanceStatus status;

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        private String notes;
    }
}
