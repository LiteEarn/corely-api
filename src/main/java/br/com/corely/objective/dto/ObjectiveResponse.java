package br.com.corely.objective.dto;

import br.com.corely.objective.ObjectiveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectiveResponse {

    private UUID id;
    private UUID studentId;
    private ObjectiveType type;
    private String description;
    private LocalDate startDate;
    private Boolean active;
}
