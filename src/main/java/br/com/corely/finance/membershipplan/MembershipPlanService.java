package br.com.corely.finance.membershipplan;

import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.finance.membershipplan.dto.MembershipPlanRequest;
import br.com.corely.finance.membershipplan.dto.MembershipPlanResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipPlanService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public MembershipPlanResponse create(MembershipPlanRequest request) {
        validateUniqueName(null, request.getName());

        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var plan = new MembershipPlan();
        plan.setStudio(studio);
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setMonthlyPrice(request.getMonthlyPrice());
        plan.setSessionsPerWeek(request.getSessionsPerWeek());
        plan.setActive(true);

        plan = membershipPlanRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public List<MembershipPlanResponse> findAll() {
        return membershipPlanRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MembershipPlanResponse> findAllActive() {
        return membershipPlanRepository.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MembershipPlanResponse findById(UUID id) {
        var plan = membershipPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membership plan not found"));
        return toResponse(plan);
    }

    @Transactional
    public MembershipPlanResponse update(UUID id, MembershipPlanRequest request) {
        var plan = membershipPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membership plan not found"));

        validateUniqueName(id, request.getName());

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setMonthlyPrice(request.getMonthlyPrice());
        plan.setSessionsPerWeek(request.getSessionsPerWeek());

        plan = membershipPlanRepository.save(plan);
        return toResponse(plan);
    }

    @Transactional
    public void delete(UUID id) {
        var plan = membershipPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membership plan not found"));

        long studentCount = membershipPlanRepository.countStudentsByPlanId(id);
        if (studentCount > 0) {
            throw new BusinessException("Cannot delete plan. There are " + studentCount + " students linked to this plan. Deactivate it instead.");
        }

        plan.setActive(false);
        membershipPlanRepository.save(plan);
    }

    private void validateUniqueName(UUID id, String name) {
        var studioId = tenantContext.getCurrentStudioId();
        boolean exists = (id != null)
                ? membershipPlanRepository.existsByNameAndIdNot(studioId, name, id)
                : membershipPlanRepository.existsByName(studioId, name);
        if (exists) {
            throw new BusinessException("An active plan with this name already exists in this studio");
        }
    }

    private MembershipPlanResponse toResponse(MembershipPlan plan) {
        return new MembershipPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getMonthlyPrice(),
                plan.getSessionsPerWeek(),
                plan.getActive(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
