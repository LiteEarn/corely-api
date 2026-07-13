package br.com.corely.comercial.delinquencypolicy;

import br.com.corely.comercial.delinquencypolicy.dto.DelinquencyActionDto;
import br.com.corely.comercial.delinquencypolicy.dto.DelinquencyPolicyRequest;
import br.com.corely.comercial.delinquencypolicy.dto.DelinquencyPolicyResponse;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DelinquencyPolicyService {

    private final DelinquencyPolicyRepository delinquencyPolicyRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public DelinquencyPolicyResponse getOrCreate() {
        var studioId = tenantContext.getCurrentStudioId();
        return delinquencyPolicyRepository.findByStudioId(studioId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    var studio = studioRepository.getReferenceById(studioId);
                    var policy = new DelinquencyPolicy();
                    policy.setStudio(studio);
                    policy.setGracePeriodDays(0);
                    policy.setAction(DelinquencyAction.NONE);
                    policy.setActive(true);
                    policy = delinquencyPolicyRepository.save(policy);
                    return toResponse(policy);
                });
    }

    @Transactional
    public DelinquencyPolicyResponse update(DelinquencyPolicyRequest request) {
        var studioId = tenantContext.getCurrentStudioId();
        var policy = delinquencyPolicyRepository.findByStudioId(studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Delinquency policy not found"));

        policy.setGracePeriodDays(request.getGracePeriodDays());
        policy.setAction(DelinquencyAction.valueOf(request.getAction().name()));

        if (request.getActive() != null) {
            policy.setActive(request.getActive());
        }

        policy = delinquencyPolicyRepository.save(policy);
        return toResponse(policy);
    }

    private DelinquencyPolicyResponse toResponse(DelinquencyPolicy policy) {
        return new DelinquencyPolicyResponse(
                policy.getId(),
                policy.getGracePeriodDays(),
                DelinquencyActionDto.valueOf(policy.getAction().name()),
                policy.getActive(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
