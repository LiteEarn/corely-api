package br.com.corely.classsession;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.instructor.Instructor;
import br.com.corely.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "class_sessions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ClassSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_group_id", nullable = false)
    private ClassGroup classGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClassSessionStatus status = ClassSessionStatus.SCHEDULED;

    @Column(name = "notes", length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 30)
    private CancelReason cancelReason;

    @Column(name = "cancel_description", length = 500)
    private String cancelDescription;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
}
