package br.com.corely.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotNull(message = "Studio ID is required")
    private UUID studioId;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Instructor ID is required")
    private UUID instructorId;

    private Long roomId;

    @NotBlank(message = "Class type is required")
    private String classType;

    @NotNull(message = "Start date/time is required")
    private LocalDateTime startDateTime;

    @NotNull(message = "End date/time is required")
    private LocalDateTime endDateTime;

    private Integer capacity;

    private Boolean makeUpClass;

    private UUID originalBookingId;
}
