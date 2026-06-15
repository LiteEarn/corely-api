package br.com.corely.enrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private UUID id;
    private UUID studioId;
    private UUID studentId;
    private String studentName;
    private UUID classGroupId;
    private String classGroupName;
    private LocalDate enrollmentDate;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
