package br.com.corely.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private boolean available;
    private String reason;
}
