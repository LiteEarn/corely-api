package br.com.corely.classsession.dto;

import br.com.corely.classsession.ClassSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionResponse {

    private UUID id;
    private String title;
    private LocalDate scheduledDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxStudents;
    private ClassSessionStatus status;
    private UUID instructorId;
    private String instructorName;
    private UUID studioId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
