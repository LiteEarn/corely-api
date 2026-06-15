package br.com.corely.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceBulkRequest {

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    @NotNull(message = "Class group ID is required")
    private UUID classGroupId;

    @NotNull(message = "Studio ID is required")
    private UUID studioId;

    @NotNull(message = "Attendances is required")
    @Valid
    private List<AttendanceItemRequest> attendances;
}
