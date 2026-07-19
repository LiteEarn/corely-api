package br.com.corely.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConflictResponse {

    private String type;
    private String description;
    private UUID conflictingBookingId;
}
