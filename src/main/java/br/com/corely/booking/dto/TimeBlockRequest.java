package br.com.corely.booking.dto;

import br.com.corely.booking.BlockType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeBlockRequest {

    @NotNull(message = "Studio ID is required")
    private UUID studioId;

    private Long instructorId;

    private Long roomId;

    @NotNull(message = "Block type is required")
    private BlockType blockType;

    private String description;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;
}
