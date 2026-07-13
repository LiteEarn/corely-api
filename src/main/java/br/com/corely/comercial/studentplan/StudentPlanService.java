package br.com.corely.comercial.studentplan;

import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.studentplan.dto.StudentPlanRequest;
import br.com.corely.comercial.studentplan.dto.StudentPlanResponse;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentPlanService {

    private final StudentPlanRepository studentPlanRepository;
    private final PlanRepository planRepository;
    private final StudentRepository studentRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public StudentPlanResponse create(StudentPlanRequest request) {
        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        var plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        if (!Boolean.TRUE.equals(plan.getActive())) {
            throw new BusinessException("Cannot enroll in an inactive plan.");
        }

        if (studentPlanRepository.existsByStudentIdAndStatus(request.getStudentId(), StudentPlanStatus.ACTIVE)) {
            throw new BusinessException("Student already has an active plan.");
        }

        var enrollment = new StudentPlan();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setPlan(plan);
        enrollment.setStartDate(request.getStartDate());
        enrollment.setEndDate(request.getEndDate());
        enrollment.setStatus(StudentPlanStatus.ACTIVE);

        enrollment.setSnapshotName(plan.getName());
        enrollment.setSnapshotValue(plan.getPrice());
        enrollment.setSnapshotDuration(plan.getDuration());
        enrollment.setSnapshotRules(null);

        enrollment = studentPlanRepository.save(enrollment);
        return toResponse(enrollment);
    }

    @Transactional
    public StudentPlanResponse cancel(UUID id) {
        var enrollment = studentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan enrollment not found"));
        if (enrollment.getStatus() != StudentPlanStatus.ACTIVE) {
            throw new BusinessException("Only active enrollments can be cancelled.");
        }
        enrollment.setStatus(StudentPlanStatus.CANCELLED);
        enrollment = studentPlanRepository.save(enrollment);
        return toResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public StudentPlanResponse findById(UUID id) {
        var enrollment = studentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan enrollment not found"));
        return toResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public List<StudentPlanResponse> findAll() {
        return studentPlanRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentPlanResponse> findByStudentId(UUID studentId) {
        return studentPlanRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentPlanResponse findActiveByStudent(UUID studentId) {
        return studentPlanRepository.findByStudentIdAndStatus(studentId, StudentPlanStatus.ACTIVE)
                .map(this::toResponse)
                .orElse(null);
    }

    private StudentPlanResponse toResponse(StudentPlan enrollment) {
        return new StudentPlanResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getFullName(),
                enrollment.getPlan() != null ? enrollment.getPlan().getId() : null,
                enrollment.getPlan() != null ? enrollment.getPlan().getName() : null,
                enrollment.getStartDate(),
                enrollment.getEndDate(),
                enrollment.getStatus(),
                enrollment.getSnapshotName(),
                enrollment.getSnapshotValue(),
                enrollment.getSnapshotDuration(),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }
}
