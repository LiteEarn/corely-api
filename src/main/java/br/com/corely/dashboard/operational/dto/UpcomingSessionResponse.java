package br.com.corely.dashboard.operational.dto;

import br.com.corely.classsession.ClassSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UpcomingSessionResponse {
    private UUID id;
    private UUID classGroupId;
    private String className;
    private UUID instructorId;
    private String instructorName;
    private LocalTime startTime;
    private LocalTime endTime;
    private long enrolledStudents;
    private ClassSessionStatus status;
}
