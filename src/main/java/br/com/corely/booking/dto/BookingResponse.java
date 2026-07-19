package br.com.corely.booking.dto;

import br.com.corely.booking.BookingStatus;
import br.com.corely.booking.CancellationReason;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private UUID id;
    private UUID studioId;
    private UUID studentId;
    private String studentName;
    private UUID instructorId;
    private String instructorName;
    private Long roomId;
    private String classType;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private BookingStatus status;
    private Integer capacity;
    private Boolean makeUpClass;
    private UUID originalBookingId;
    private CancellationReason cancellationReason;
    private String cancellationNotes;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
