package br.com.corely.enrollment;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.shared.audit.BaseEntity;
import br.com.corely.student.Student;
import br.com.corely.studio.Studio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "enrollments")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Enrollment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_group_id", nullable = false)
    private ClassGroup classGroup;

    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
