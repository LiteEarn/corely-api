package br.com.corely.instructor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReassignRequest {

    @NotNull(message = "Target instructor ID is required")
    private UUID targetInstructorId;

    private List<UUID> classGroupIds;
}
