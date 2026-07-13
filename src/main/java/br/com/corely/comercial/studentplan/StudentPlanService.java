package br.com.corely.comercial.studentplan;

import br.com.corely.comercial.contractsnapshot.ContractSnapshotService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentPlanService {

    private final StudentPlanRepository studentPlanRepository;
    private final StudentRepository studentRepository;
    private final StudioRepository studioRepository;
    private final ContractSnapshotService contractSnapshotService;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public StudentPlanResponse create(StudentPlanRequest request) {
        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (studentPlanRepository.existsByStudentIdAndStatus(request.getStudentId(), StudentPlanStatus.ACTIVE)) {
            throw new BusinessException("Student already has an active plan.");
        }

        var snapshot = contractSnapshotService.create(request.getPlanId());

        var enrollment = new StudentPlan();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setContractSnapshot(snapshot);
        enrollment.setStartDate(request.getStartDate());
        enrollment.setEndDate(request.getEndDate());
        enrollment.setStatus(StudentPlanStatus.ACTIVE);

        enrollment = studentPlanRepository.save(enrollment);

        return toResponse(enrollment);
    }

    @Transactional
    public StudentPlanResponse cancel(UUID id) {
        return transitionStatus(id, StudentPlanStatus.ACTIVE, StudentPlanStatus.CANCELLED);
    }

    @Transactional
    public StudentPlanResponse suspend(UUID id) {
        return transitionStatus(id, StudentPlanStatus.ACTIVE, StudentPlanStatus.SUSPENDED);
    }

    @Transactional
    public StudentPlanResponse reactivate(UUID id) {
        return transitionStatus(id, StudentPlanStatus.SUSPENDED, StudentPlanStatus.ACTIVE);
    }

    private StudentPlanResponse transitionStatus(UUID id, StudentPlanStatus from, StudentPlanStatus to) {
        var enrollment = studentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudentPlan not found"));

        if (enrollment.getStatus() != from) {
            throw new BusinessException("StudentPlan must be " + from + " to transition to " + to);
        }

        if ((to == StudentPlanStatus.CANCELLED || to == StudentPlanStatus.SUSPENDED)
                && enrollment.getCancellationDate() == null) {
            enrollment.setCancellationDate(LocalDate.now());
        }

        if (to == StudentPlanStatus.ACTIVE) {
            enrollment.setCancellationDate(null);
            enrollment.setCancellationReason(null);
        }

        if (studentPlanRepository.existsByStudentIdAndStatus(enrollment.getStudent().getId(), to)) {
            throw new BusinessException("Student already has a plan with status " + to);
        }

        enrollment.setStatus(to);
        enrollment = studentPlanRepository.save(enrollment);
        return toResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public StudentPlanResponse findById(UUID id) {
        var enrollment = studentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudentPlan not found"));
        return toResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public List<StudentPlanResponse> findAll() {
        return studentPlanRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentPlan findEntityById(UUID id) {
        return studentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudentPlan not found"));
    }

    @Transactional(readOnly = true)
    public StudentPlanResponse findActiveByStudent(UUID studentId) {
        return studentPlanRepository.findByStudentIdAndStatus(studentId, StudentPlanStatus.ACTIVE)
                .map(this::toResponse)
                .orElse(null);
    }

    private StudentPlanResponse toResponse(StudentPlan enrollment) {
        var snapshot = enrollment.getContractSnapshot();
        return new StudentPlanResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getFullName(),
                snapshot.getId(),
                snapshot.getPlanName(),
                enrollment.getStartDate(),
                enrollment.getEndDate(),
                enrollment.getStatus(),
                enrollment.getCancellationDate(),
                enrollment.getCancellationReason(),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }
}
