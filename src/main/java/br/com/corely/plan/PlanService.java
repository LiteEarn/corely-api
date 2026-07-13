package br.com.corely.plan;

import br.com.corely.plan.dto.PlanRequest;
import br.com.corely.plan.dto.PlanResponse;
import br.com.corely.plan.dto.PlanType;
import br.com.corely.planenrollment.PlanEnrollmentRepository;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
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
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanEnrollmentRepository planEnrollmentRepository;
    private final StudioRepository studioRepository;

    @Transactional
    public PlanResponse create(PlanRequest request) {
        validatePackageType(request);

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Plan plan = new Plan();
        plan.setStudio(studio);
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setType(request.getType());
        plan.setValue(request.getValue());
        plan.setQuantityAulas(request.getQuantityAulas());
        plan.setDuration(request.getDuration());
        plan.setActive(request.getActive() != null ? request.getActive() : true);

        plan = planRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> findAll() {
        return planRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> findAllActive() {
        return planRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlanResponse findById(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        return toResponse(plan);
    }

    @Transactional
    public PlanResponse update(UUID id, PlanRequest request) {
        validatePackageType(request);

        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        plan.setStudio(studio);
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setType(request.getType());
        plan.setValue(request.getValue());
        plan.setQuantityAulas(request.getQuantityAulas());
        plan.setDuration(request.getDuration());
        if (request.getActive() != null) {
            plan.setActive(request.getActive());
        }

        plan = planRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public void delete(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        boolean hasActiveEnrollments = planEnrollmentRepository.existsByPlanIdAndStatus(
                id, br.com.corely.planenrollment.dto.PlanEnrollmentStatus.ACTIVE);
        if (hasActiveEnrollments) {
            throw new BusinessException("Cannot delete plan with active enrollments.");
        }

        planRepository.delete(plan);
    }

    @Transactional
    public void inactivate(UUID id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        boolean hasActiveEnrollments = planEnrollmentRepository.existsByPlanIdAndStatus(
                id, br.com.corely.planenrollment.dto.PlanEnrollmentStatus.ACTIVE);
        if (hasActiveEnrollments) {
            throw new BusinessException("Cannot inactivate plan with active enrollments.");
        }

        plan.setActive(false);
        planRepository.save(plan);
    }

    private void validatePackageType(PlanRequest request) {
        if (request.getType() == PlanType.PACKAGE && request.getQuantityAulas() == null) {
            throw new BusinessException("Quantity of classes is required for package plans.");
        }
    }

    private PlanResponse toResponse(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getStudio().getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getType(),
                plan.getValue(),
                plan.getQuantityAulas(),
                plan.getDuration(),
                plan.getActive(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
