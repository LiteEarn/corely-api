package br.com.corely.planenrollment;

import br.com.corely.plan.Plan;
import br.com.corely.planenrollment.dto.PlanEnrollmentStatus;
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
@Table(name = "plan_enrollments")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PlanEnrollment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PlanEnrollmentStatus status = PlanEnrollmentStatus.ACTIVE;
}
