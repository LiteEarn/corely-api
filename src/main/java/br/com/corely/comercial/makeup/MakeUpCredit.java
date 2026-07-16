package br.com.corely.comercial.makeup;

import br.com.corely.comercial.ComercialBaseEntity;
import br.com.corely.comercial.attendance.Attendance;
import br.com.corely.comercial.booking.Booking;
import br.com.corely.comercial.classsession.ClassSession;
import br.com.corely.student.Student;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;

@Entity(name = "ComercialMakeUpCredit")
@Table(name = "comercial_makeup_credits")
@Filter(name = "comercialTenantFilter", condition = "studio_id = :studioId")
public class MakeUpCredit extends ComercialBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_attendance_id", nullable = false)
    private Attendance originalAttendance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_class_session_id", nullable = false)
    private ClassSession originalClassSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "makeup_booking_id")
    private Booking makeUpBooking;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MakeUpCreditStatus status = MakeUpCreditStatus.AVAILABLE;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public MakeUpCredit() {}

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Attendance getOriginalAttendance() { return originalAttendance; }
    public void setOriginalAttendance(Attendance originalAttendance) { this.originalAttendance = originalAttendance; }
    public ClassSession getOriginalClassSession() { return originalClassSession; }
    public void setOriginalClassSession(ClassSession originalClassSession) { this.originalClassSession = originalClassSession; }
    public Booking getMakeUpBooking() { return makeUpBooking; }
    public void setMakeUpBooking(Booking makeUpBooking) { this.makeUpBooking = makeUpBooking; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
    public MakeUpCreditStatus getStatus() { return status; }
    public void setStatus(MakeUpCreditStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
