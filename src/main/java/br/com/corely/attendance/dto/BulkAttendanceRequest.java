package br.com.corely.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class BulkAttendanceRequest {

    @NotNull
    private UUID studioId;

    @NotNull
    private UUID classGroupId;

    @NotNull
    private LocalDate attendanceDate;

    @NotEmpty
    private @Valid List<AttendanceItem> attendances;

    @Data
    public static class AttendanceItem {
        @NotNull
        private UUID studentId;
        private boolean present;
        private String observation;
    }
}
