package br.com.corely.classsession.dto;

import br.com.corely.classsession.ClassSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionResponse {

    private UUID id;
    private UUID classGroupId;
    private String classGroupName;
    private UUID instructorId;
    private String instructorName;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private ClassSessionStatus status;
    private String notes;
    private UUID studioId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
