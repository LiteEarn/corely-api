package br.com.corely.classsession.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionRequest {

    @NotNull(message = "Class group ID is required")
    private UUID classGroupId;

    @NotNull(message = "Session date is required")
    private LocalDate sessionDate;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
