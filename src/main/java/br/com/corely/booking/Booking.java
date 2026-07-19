package br.com.corely.booking;

import br.com.corely.shared.audit.BaseEntity;
import br.com.corely.student.Student;
import br.com.corely.studio.Studio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private br.com.corely.instructor.Instructor instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "class_type", nullable = false)
    private String classType;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status = BookingStatus.SCHEDULED;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "make_up_class", nullable = false)
    private Boolean makeUpClass = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_booking_id")
    private Booking originalBooking;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason", length = 20)
    private CancellationReason cancellationReason;

    @Column(name = "cancellation_notes", columnDefinition = "TEXT")
    private String cancellationNotes;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
