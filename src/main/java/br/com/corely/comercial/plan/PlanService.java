package br.com.corely.comercial.plan;

import br.com.corely.comercial.plan.dto.PlanRequest;
import br.com.corely.comercial.plan.dto.PlanResponse;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public PlanResponse create(PlanRequest request) {
        var studioId = tenantContext.getCurrentStudioId();
        var studio = studioRepository.getReferenceById(studioId);

        var plan = new Plan();
        plan.setStudio(studio);
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setValue(request.getValue());
        plan.setDuration(request.getDuration());
        plan.setVersion(1);
        plan.setActive(request.getActive() != null ? request.getActive() : true);

        plan = planRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> findAll() {
        var studioId = tenantContext.getCurrentStudioId();
        return planRepository.findByStudioIdOrderByName(studioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> findAllActive() {
        var studioId = tenantContext.getCurrentStudioId();
        return planRepository.findByStudioIdAndActiveTrueOrderByName(studioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse findById(UUID id) {
        var studioId = tenantContext.getCurrentStudioId();
        var plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        if (!plan.getStudio().getId().equals(studioId)) {
            throw new ResourceNotFoundException("Plan not found");
        }
        return toResponse(plan);
    }

    @Transactional
    public PlanResponse update(UUID id, PlanRequest request) {
        var studioId = tenantContext.getCurrentStudioId();
        var plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        if (!plan.getStudio().getId().equals(studioId)) {
            throw new ResourceNotFoundException("Plan not found");
        }

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setValue(request.getValue());
        plan.setDuration(request.getDuration());
        plan.setVersion(plan.getVersion() + 1);
        if (request.getActive() != null) {
            plan.setActive(request.getActive());
        }

        plan = planRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public void delete(UUID id) {
        var studioId = tenantContext.getCurrentStudioId();
        var plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        if (!plan.getStudio().getId().equals(studioId)) {
            throw new ResourceNotFoundException("Plan not found");
        }
        planRepository.delete(plan);
    }

    @Transactional
    public void inactivate(UUID id) {
        var studioId = tenantContext.getCurrentStudioId();
        var plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        if (!plan.getStudio().getId().equals(studioId)) {
            throw new ResourceNotFoundException("Plan not found");
        }
        plan.setActive(false);
        plan.setVersion(plan.getVersion() + 1);
        planRepository.save(plan);
    }

    private PlanResponse toResponse(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getValue(),
                plan.getDuration(),
                plan.getVersion(),
                plan.getActive(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
