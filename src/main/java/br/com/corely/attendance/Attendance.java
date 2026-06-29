package br.com.corely.attendance;

import br.com.corely.classsession.ClassSession;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "attendances")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Attendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "notes", length = 500)
    private String notes;
}
