package br.com.corely.attendance;

import br.com.corely.attendance.dto.AttendanceRequest;
import br.com.corely.attendance.dto.AttendanceResponse;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.shared.exception.ConflictException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassSessionRepository classSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassGroupRepository classGroupRepository;

    @Transactional
    public AttendanceResponse register(UUID sessionId, AttendanceRequest request) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Class session not found"));

        if (session.getStatus() == ClassSessionStatus.COMPLETED) {
            throw new ConflictException("A presença não pode ser registrada após a conclusão da aula.");
        }

        if (session.getStatus() != ClassSessionStatus.IN_PROGRESS) {
            throw new ConflictException("A presença somente pode ser registrada durante a aula.");
        }

        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!Boolean.TRUE.equals(enrollment.getActive())) {
            throw new ConflictException("Matrícula inativa.");
        }

        if (!enrollment.getClassGroup().getId().equals(session.getClassGroup().getId())) {
            throw new ConflictException("Matrícula não pertence à turma da sessão.");
        }

        Attendance attendance = attendanceRepository
                .findByClassSessionIdAndEnrollmentId(sessionId, enrollment.getId())
                .orElse(null);

        if (attendance == null) {
            attendance = new Attendance();
            attendance.setClassSession(session);
            attendance.setEnrollment(enrollment);
        }

        attendance.setStatus(request.getStatus());
        attendance.setNotes(request.getNotes());

        attendance = attendanceRepository.save(attendance);
        return toResponse(attendance);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findBySessionId(UUID sessionId) {
        if (!classSessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Class session not found");
        }
        return attendanceRepository.findByClassSessionId(sessionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findByEnrollmentId(UUID enrollmentId) {
        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new ResourceNotFoundException("Enrollment not found");
        }
        return attendanceRepository.findByEnrollmentId(enrollmentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findByClassGroupAndDate(UUID classGroupId, LocalDate date) {
        ClassGroup classGroup = classGroupRepository.findById(classGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));
        UUID studioId = classGroup.getStudio().getId();
        return attendanceRepository.findByClassGroupIdAndDate(classGroupId, date, studioId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getClassSession().getId(),
                attendance.getEnrollment().getId(),
                attendance.getEnrollment().getStudent().getFullName(),
                attendance.getStatus(),
                attendance.getNotes(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }
}
