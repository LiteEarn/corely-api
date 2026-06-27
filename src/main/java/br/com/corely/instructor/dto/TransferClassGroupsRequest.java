package br.com.corely.instructor.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferClassGroupsRequest {

    @NotNull(message = "Target instructor ID is required")
    private UUID targetInstructorId;

    @NotEmpty(message = "Class group IDs cannot be empty")
    private List<UUID> classGroupIds;
}
