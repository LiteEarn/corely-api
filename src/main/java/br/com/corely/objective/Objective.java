package br.com.corely.objective;

import br.com.corely.shared.audit.BaseEntity;
import br.com.corely.student.Student;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "objectives")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Objective extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ObjectiveType type;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
