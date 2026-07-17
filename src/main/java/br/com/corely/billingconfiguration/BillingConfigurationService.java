package br.com.corely.billingconfiguration;

import br.com.corely.billingconfiguration.dto.BillingConfigurationRequest;
import br.com.corely.billingconfiguration.dto.BillingConfigurationResponse;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingConfigurationService {

    private final BillingConfigurationRepository billingConfigurationRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional(readOnly = true)
    public BillingConfigurationResponse findByCurrentStudio() {
        var studioId = tenantContext.getCurrentStudioId();
        var config = billingConfigurationRepository.findByStudioId(studioId)
                .orElseThrow(() -> new ResourceNotFoundException("BillingConfiguration not found for this studio"));
        return toResponse(config);
    }

    @Transactional
    public BillingConfigurationResponse save(BillingConfigurationRequest request) {
        var studioId = tenantContext.getCurrentStudioId();
        var studio = studioRepository.getReferenceById(studioId);

        var config = billingConfigurationRepository.findByStudioId(studioId)
                .orElse(new BillingConfiguration());

        config.setStudio(studio);
        config.setDueDay(request.getDueDay());
        config.setDefaultAmount(request.getDefaultAmount());
        if (request.getActive() != null) {
            config.setActive(request.getActive());
        }

        config = billingConfigurationRepository.save(config);
        return toResponse(config);
    }

    private BillingConfigurationResponse toResponse(BillingConfiguration config) {
        return new BillingConfigurationResponse(
                config.getId(),
                config.getStudio().getId(),
                config.getDueDay(),
                config.getDefaultAmount(),
                config.getActive(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
