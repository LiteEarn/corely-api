package br.com.corely.booking.dto;

import br.com.corely.booking.BlockType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeBlockResponse {

    private UUID id;
    private UUID studioId;
    private Long instructorId;
    private Long roomId;
    private BlockType blockType;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
