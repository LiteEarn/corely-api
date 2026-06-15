package br.com.corely.classsession;

import br.com.corely.instructor.Instructor;
import br.com.corely.shared.audit.BaseEntity;
import br.com.corely.studio.Studio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "class_sessions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ClassSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "max_students", nullable = false)
    private Integer maxStudents;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClassSessionStatus status = ClassSessionStatus.SCHEDULED;
}
