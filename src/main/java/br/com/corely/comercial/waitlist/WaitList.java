package br.com.corely.comercial.waitlist;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.comercial.classsession.ClassSession;
import br.com.corely.student.Student;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

@Entity(name = "ComercialWaitList")
@Table(name = "comercial_wait_list")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class WaitList extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WaitListStatus status = WaitListStatus.WAITING;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public WaitList() {}

    public ClassSession getClassSession() { return classSession; }
    public void setClassSession(ClassSession classSession) { this.classSession = classSession; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public WaitListStatus getStatus() { return status; }
    public void setStatus(WaitListStatus status) { this.status = status; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
