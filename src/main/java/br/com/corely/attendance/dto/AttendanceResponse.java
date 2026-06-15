package br.com.corely.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    private UUID id;
    private UUID studentId;
    private String studentName;
    private UUID classGroupId;
    private String classGroupName;
    private LocalDate attendanceDate;
    private Boolean present;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
