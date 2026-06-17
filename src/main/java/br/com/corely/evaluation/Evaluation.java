package br.com.corely.evaluation;

import br.com.corely.shared.audit.BaseEntity;
import br.com.corely.student.Student;
import br.com.corely.studio.Studio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "evaluations")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Evaluation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studio_id", nullable = false)
    private Studio studio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "evaluation_date", nullable = false)
    private LocalDate evaluationDate;

    @Column(name = "weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "height", nullable = false, precision = 4, scale = 2)
    private BigDecimal height;

    @Column(name = "observations", length = 1000)
    private String observations;
}
