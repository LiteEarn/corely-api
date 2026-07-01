package br.com.corely.makeup;

import br.com.corely.attendance.Attendance;
import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.attendance.AttendanceStatus;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.student.Student;
import br.com.corely.makeup.dto.MakeupApproveRequest;
import br.com.corely.makeup.dto.MakeupRejectRequest;
import br.com.corely.makeup.dto.MakeupRequestRequest;
import br.com.corely.makeup.dto.MakeupRequestResponse;
import br.com.corely.shared.exception.ConflictException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MakeupRequestService {

    private final MakeupRequestRepository makeupRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassSessionRepository classSessionRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public MakeupRequestResponse request(UUID attendanceId, MakeupRequestRequest request) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));

        if (attendance.getStatus() != AttendanceStatus.ABSENT) {
            throw new ConflictException(
                    "Makeup request is only allowed for absent attendances. Current status: " + attendance.getStatus()
            );
        }

        if (makeupRequestRepository.existsByAttendanceId(attendanceId)) {
            throw new ConflictException("A makeup request already exists for this attendance");
        }

        MakeupRequest makeupRequest = new MakeupRequest();
        makeupRequest.setAttendance(attendance);
        makeupRequest.setStatus(MakeupRequestStatus.REQUESTED);
        makeupRequest.setReason(request.getReason());
        makeupRequest.setRequestedAt(LocalDateTime.now());

        makeupRequest = makeupRequestRepository.save(makeupRequest);
        return toResponse(makeupRequest);
    }

    @Transactional(readOnly = true)
    public MakeupRequestResponse findByAttendanceId(UUID attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new ResourceNotFoundException("Attendance not found");
        }

        return makeupRequestRepository.findByAttendanceId(attendanceId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Makeup request not found for this attendance"));
    }

    @Transactional(readOnly = true)
    public List<MakeupRequestResponse> findAll(MakeupRequestStatus status, UUID studentId, UUID classGroupId) {
        if (status != null) {
            return makeupRequestRepository.findByStatus(status).stream()
                    .map(this::toResponse)
                    .toList();
        }

        return makeupRequestRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MakeupRequestResponse approve(UUID id, MakeupApproveRequest request) {
        MakeupRequest makeupRequest = makeupRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Makeup request not found"));

        if (makeupRequest.getStatus() != MakeupRequestStatus.REQUESTED) {
            throw new ConflictException("A reposição já foi processada.");
        }

        ClassSession targetSession = classSessionRepository.findById(request.getTargetSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Class session not found"));

        validateTargetSession(targetSession);

        Student student = makeupRequest.getAttendance().getEnrollment().getStudent();
        ClassGroup targetGroup = targetSession.getClassGroup();

        Optional<Enrollment> targetEnrollment = enrollmentRepository
                .findByStudentIdAndClassGroupId(student.getId(), targetGroup.getId());

        if (targetEnrollment.isPresent() && targetEnrollment.get().getActive()) {
            boolean alreadyAttending = attendanceRepository
                    .existsByClassSessionIdAndEnrollmentId(targetSession.getId(), targetEnrollment.get().getId());
            if (alreadyAttending) {
                throw new ConflictException("O aluno já participa desta aula.");
            }
        }

        long enrollmentCount = enrollmentRepository.countByClassGroupIdAndActiveTrue(targetGroup.getId());
        if (enrollmentCount >= targetGroup.getCapacity()) {
            throw new ConflictException("A turma está lotada.");
        }

        makeupRequest.setStatus(MakeupRequestStatus.APPROVED);
        makeupRequest.setTargetSession(targetSession);
        makeupRequest.setApprovedAt(LocalDateTime.now());

        makeupRequest = makeupRequestRepository.save(makeupRequest);
        return toResponse(makeupRequest);
    }

    @Transactional
    public MakeupRequestResponse reject(UUID id, MakeupRejectRequest request) {
        MakeupRequest makeupRequest = makeupRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Makeup request not found"));

        if (makeupRequest.getStatus() != MakeupRequestStatus.REQUESTED) {
            throw new ConflictException("A reposição já foi processada.");
        }

        makeupRequest.setStatus(MakeupRequestStatus.REJECTED);
        makeupRequest.setRejectedAt(LocalDateTime.now());
        makeupRequest.setRejectionReason(request.getReason());

        makeupRequest = makeupRequestRepository.save(makeupRequest);
        return toResponse(makeupRequest);
    }

    private void validateTargetSession(ClassSession targetSession) {
        if (targetSession.getStatus() == ClassSessionStatus.CANCELLED) {
            throw new ConflictException("A aula escolhida está cancelada.");
        }
        if (targetSession.getStatus() == ClassSessionStatus.COMPLETED) {
            throw new ConflictException("A aula escolhida já foi concluída.");
        }
        if (targetSession.getStatus() == ClassSessionStatus.IN_PROGRESS) {
            throw new ConflictException("A aula escolhida já iniciou.");
        }
        if (targetSession.getSessionDate().isBefore(LocalDate.now())) {
            throw new ConflictException("A reposição deve ser marcada para uma aula futura.");
        }
    }

    private MakeupRequestResponse toResponse(MakeupRequest makeupRequest) {
        return new MakeupRequestResponse(
                makeupRequest.getId(),
                makeupRequest.getAttendance().getId(),
                makeupRequest.getTargetSession() != null ? makeupRequest.getTargetSession().getId() : null,
                makeupRequest.getStatus(),
                makeupRequest.getReason(),
                makeupRequest.getRequestedAt(),
                makeupRequest.getApprovedAt(),
                makeupRequest.getRejectedAt(),
                makeupRequest.getRejectionReason(),
                makeupRequest.getCreatedAt(),
                makeupRequest.getUpdatedAt()
        );
    }
}
