package br.com.corely.makeup;

import br.com.corely.attendance.Attendance;
import br.com.corely.classsession.ClassSession;
import br.com.corely.shared.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "makeup_requests")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MakeupRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_session_id")
    private ClassSession targetSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MakeupRequestStatus status;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
