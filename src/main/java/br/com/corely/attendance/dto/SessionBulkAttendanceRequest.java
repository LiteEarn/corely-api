package br.com.corely.attendance.dto;

import br.com.corely.attendance.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SessionBulkAttendanceRequest {

    @NotNull
    private @Valid List<AttendanceItem> attendances;

    @Data
    public static class AttendanceItem {
        @NotNull
        private UUID enrollmentId;

        @NotNull
        private AttendanceStatus status;
    }
}
