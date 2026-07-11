package br.com.corely.classgroup;

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
@Table(name = "class_groups")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ClassGroup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "monday", nullable = false)
    private Boolean monday = false;

    @Column(name = "tuesday", nullable = false)
    private Boolean tuesday = false;

    @Column(name = "wednesday", nullable = false)
    private Boolean wednesday = false;

    @Column(name = "thursday", nullable = false)
    private Boolean thursday = false;

    @Column(name = "friday", nullable = false)
    private Boolean friday = false;

    @Column(name = "saturday", nullable = false)
    private Boolean saturday = false;

    @Column(name = "sunday", nullable = false)
    private Boolean sunday = false;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
