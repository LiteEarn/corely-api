package br.com.corely.instructor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstructorResponse {

    private UUID id;
    private UUID studioId;
    private String fullName;
    private String email;
    private String phone;
    private String specialty;
    private Boolean active;
}
