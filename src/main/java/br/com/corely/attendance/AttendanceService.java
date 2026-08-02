package br.com.corely.attendance;

import br.com.corely.attendance.dto.AttendanceRequest;
import br.com.corely.attendance.dto.AttendanceResponse;
import br.com.corely.attendance.dto.BulkAttendanceRequest;
import br.com.corely.attendance.dto.BulkAttendanceResponse;
import br.com.corely.attendance.dto.SessionAttendanceResponse;
import br.com.corely.attendance.dto.SessionBulkAttendanceRequest;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.shared.exception.ConflictException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassSessionRepository classSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassGroupRepository classGroupRepository;
    private final TenantContext tenantContext;

    @Transactional
    public AttendanceResponse register(UUID sessionId, AttendanceRequest request) {
        UUID studioId = tenantContext.getCurrentStudioId();
        ClassSession session = classSessionRepository.findByIdAndStudioId(sessionId, studioId)
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
                .findByClassSessionIdAndEnrollmentIdAndStudioId(sessionId, enrollment.getId(), studioId)
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
        UUID studioId = tenantContext.getCurrentStudioId();
        if (!classSessionRepository.findByIdAndStudioId(sessionId, studioId).isPresent()) {
            throw new ResourceNotFoundException("Class session not found");
        }
        return attendanceRepository.findByClassSessionIdAndStudioId(sessionId, studioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findByEnrollmentId(UUID enrollmentId) {
        UUID studioId = tenantContext.getCurrentStudioId();
        if (!enrollmentRepository.existsById(enrollmentId)) {
            throw new ResourceNotFoundException("Enrollment not found");
        }
        return attendanceRepository.findByEnrollmentIdAndStudioId(enrollmentId, studioId).stream()
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

    @Transactional
    public BulkAttendanceResponse bulkSave(BulkAttendanceRequest request) {
        UUID studioId = tenantContext.getCurrentStudioId();
        ClassSession session = classSessionRepository
                .findFirstByClassGroupIdAndSessionDateAndStatusOrderByStartTimeAndStudioId(
                        request.getClassGroupId(), request.getAttendanceDate(), ClassSessionStatus.IN_PROGRESS, studioId)
                .orElseThrow(() -> new ConflictException("Nenhuma sessão em andamento encontrada para esta turma e data."));

        int savedCount = 0;

        for (var item : request.getAttendances()) {
            Enrollment enrollment = enrollmentRepository
                    .findByStudentIdAndClassGroupId(item.getStudentId(), request.getClassGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Matrícula não encontrada para o studentId: " + item.getStudentId()));

            if (!Boolean.TRUE.equals(enrollment.getActive())) {
                throw new ConflictException("Matrícula inativa.");
            }

            if (!enrollment.getStudio().getId().equals(request.getStudioId())) {
                throw new ConflictException("Matrícula não pertence ao studio informado.");
            }

            Attendance attendance = attendanceRepository
                    .findByClassSessionIdAndEnrollmentIdAndStudioId(session.getId(), enrollment.getId(), studioId)
                    .orElse(null);

            if (attendance == null) {
                attendance = new Attendance();
                attendance.setClassSession(session);
                attendance.setEnrollment(enrollment);
            }

            attendance.setStatus(item.isPresent() ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT);
            attendance.setNotes(item.getObservation());

            attendanceRepository.save(attendance);
            savedCount++;
        }

        return new BulkAttendanceResponse(savedCount + " presença(s) salva(s) com sucesso.", savedCount);
    }

    @Transactional
    public List<SessionAttendanceResponse> saveSessionAttendances(
            UUID sessionId, SessionBulkAttendanceRequest request) {
        UUID studioId = tenantContext.getCurrentStudioId();
        ClassSession session = classSessionRepository.findByIdAndStudioId(sessionId, studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Class session not found"));

        if (session.getStatus() == ClassSessionStatus.COMPLETED) {
            throw new ConflictException("A presença não pode ser registrada após a conclusão da aula.");
        }

        List<SessionAttendanceResponse> responses = new ArrayList<>();

        for (var item : request.getAttendances()) {
            Enrollment enrollment = enrollmentRepository.findById(item.getEnrollmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

            if (!Boolean.TRUE.equals(enrollment.getActive())) {
                throw new ConflictException("Matrícula inativa.");
            }

            if (!enrollment.getClassGroup().getId().equals(session.getClassGroup().getId())) {
                throw new ConflictException("Matrícula não pertence à turma da sessão.");
            }

            Attendance attendance = attendanceRepository
                    .findByClassSessionIdAndEnrollmentIdAndStudioId(sessionId, enrollment.getId(), studioId)
                    .orElse(null);

            if (attendance == null) {
                attendance = new Attendance();
                attendance.setClassSession(session);
                attendance.setEnrollment(enrollment);
            }

            attendance.setStatus(item.getStatus());
            attendance.setNotes(null);

            attendance = attendanceRepository.save(attendance);
            responses.add(toSessionAttendanceResponse(attendance));
        }

        return responses;
    }

    private SessionAttendanceResponse toSessionAttendanceResponse(Attendance attendance) {
        return new SessionAttendanceResponse(
                attendance.getId(),
                attendance.getClassSession().getId(),
                attendance.getEnrollment().getId(),
                attendance.getEnrollment().getStudent().getId(),
                attendance.getEnrollment().getStudent().getFullName(),
                attendance.getStatus(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
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
