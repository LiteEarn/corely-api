package br.com.corely.planenrollment;

import br.com.corely.plan.Plan;
import br.com.corely.plan.PlanRepository;
import br.com.corely.planenrollment.dto.PlanEnrollmentRequest;
import br.com.corely.planenrollment.dto.PlanEnrollmentResponse;
import br.com.corely.planenrollment.dto.PlanEnrollmentStatus;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanEnrollmentService {

    private final PlanEnrollmentRepository planEnrollmentRepository;
    private final PlanRepository planRepository;
    private final StudentRepository studentRepository;
    private final StudioRepository studioRepository;

    @Transactional
    public PlanEnrollmentResponse create(PlanEnrollmentRequest request) {
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        if (!Boolean.TRUE.equals(plan.getActive())) {
            throw new BusinessException("Cannot enroll in an inactive plan.");
        }

        if (planEnrollmentRepository.existsByStudentIdAndStatus(request.getStudentId(), PlanEnrollmentStatus.ACTIVE)) {
            throw new BusinessException("Student already has an active plan enrollment.");
        }

        PlanEnrollment enrollment = new PlanEnrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setPlan(plan);
        enrollment.setStartDate(request.getStartDate());
        enrollment.setEndDate(request.getEndDate());
        enrollment.setStatus(PlanEnrollmentStatus.ACTIVE);

        enrollment = planEnrollmentRepository.save(enrollment);
        return toResponse(enrollment);
    }

    @Transactional
    public PlanEnrollmentResponse cancel(UUID id) {
        PlanEnrollment enrollment = planEnrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan enrollment not found"));

        if (enrollment.getStatus() != PlanEnrollmentStatus.ACTIVE) {
            throw new BusinessException("Only active enrollments can be cancelled.");
        }

        enrollment.setStatus(PlanEnrollmentStatus.CANCELLED);
        enrollment = planEnrollmentRepository.save(enrollment);
        return toResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public PlanEnrollmentResponse findById(UUID id) {
        PlanEnrollment enrollment = planEnrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan enrollment not found"));
        return toResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public List<PlanEnrollmentResponse> findAll() {
        return planEnrollmentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlanEnrollmentResponse> findByStudentId(UUID studentId) {
        return planEnrollmentRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public br.com.corely.planenrollment.dto.StudentPlanResponse findActiveByStudent(UUID studentId) {
        var opt = planEnrollmentRepository.findByStudentIdAndStatus(studentId, PlanEnrollmentStatus.ACTIVE);
        if (opt.isEmpty()) {
            return null;
        }
        PlanEnrollment enrollment = opt.get();
        return new br.com.corely.planenrollment.dto.StudentPlanResponse(
                enrollment.getId(),
                enrollment.getPlan().getId(),
                enrollment.getPlan().getName(),
                enrollment.getPlan().getType().name(),
                enrollment.getPlan().getValue(),
                enrollment.getStartDate(),
                enrollment.getEndDate(),
                enrollment.getStatus(),
                enrollment.getCreatedAt()
        );
    }

    private PlanEnrollmentResponse toResponse(PlanEnrollment enrollment) {
        return new PlanEnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudio().getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getFullName(),
                enrollment.getPlan().getId(),
                enrollment.getPlan().getName(),
                enrollment.getPlan().getValue(),
                enrollment.getStartDate(),
                enrollment.getEndDate(),
                enrollment.getStatus(),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }
}
