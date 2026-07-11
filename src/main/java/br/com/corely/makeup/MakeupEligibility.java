package br.com.corely.makeup;

import br.com.corely.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "makeup_eligibility")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MakeupEligibility extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MakeupEligibilityStatus status = MakeupEligibilityStatus.ELIGIBLE;
}
