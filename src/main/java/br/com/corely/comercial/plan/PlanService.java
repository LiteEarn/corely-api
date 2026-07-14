package br.com.corely.comercial.plan;

import br.com.corely.comercial.plan.dto.PlanRequest;
import br.com.corely.comercial.plan.dto.PlanResponse;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public PlanResponse create(PlanRequest request) {
        validateUniqueName(null, request.getName());

        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var plan = new Plan();
        plan.setStudio(studio);
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setDuration(request.getDuration());
        plan.setVersion(1);
        plan.setActive(request.getActive() != null ? request.getActive() : true);
        plan.setAutoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : true);

        plan = planRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public Page<PlanResponse> findAll(String name, Boolean active, Pageable pageable) {
        Page<Plan> plans;

        if (name != null && active != null) {
            plans = planRepository.findByNameContainingIgnoreCaseAndActive(name, active, pageable);
        } else if (name != null) {
            plans = planRepository.findByNameContainingIgnoreCase(name, pageable);
        } else if (active != null) {
            plans = planRepository.findByActive(active, pageable);
        } else {
            plans = planRepository.findAll(pageable);
        }

        return plans.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PlanResponse findById(UUID id) {
        var plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        return toResponse(plan);
    }

    @Transactional
    public PlanResponse update(UUID id, PlanRequest request) {
        var plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        validateUniqueName(id, request.getName());

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setDuration(request.getDuration());
        plan.setVersion(plan.getVersion() + 1);
        if (request.getActive() != null) {
            plan.setActive(request.getActive());
        }
        if (request.getAutoRenew() != null) {
            plan.setAutoRenew(request.getAutoRenew());
        }

        plan = planRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public void activate(UUID id) {
        var plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        if (plan.getActive()) {
            throw new BusinessException("Plan is already active");
        }
        plan.setActive(true);
        plan.setVersion(plan.getVersion() + 1);
        planRepository.save(plan);
    }

    @Transactional
    public void inactivate(UUID id) {
        var plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        if (!plan.getActive()) {
            throw new BusinessException("Plan is already inactive");
        }
        plan.setActive(false);
        plan.setVersion(plan.getVersion() + 1);
        planRepository.save(plan);
    }

    private void validateUniqueName(UUID id, String name) {
        var studioId = tenantContext.getCurrentStudioId();
        boolean exists = (id != null)
                ? planRepository.existsByStudioIdAndNameAndIdNot(studioId, name, id)
                : planRepository.existsByStudioIdAndName(studioId, name);
        if (exists) {
            throw new BusinessException("Plan name already exists: " + name);
        }
    }

    private PlanResponse toResponse(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getDuration(),
                plan.getVersion(),
                plan.getActive(),
                plan.getAutoRenew(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
