package br.com.corely.comercial.tenant;

import br.com.corely.auth.security.AuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ComercialTenantContext {

    private final AuthenticationFacade authenticationFacade;

    public UUID getCurrentStudioId() {
        UUID studioId = authenticationFacade.getCurrentStudioId();
        if (studioId == null) {
            throw new TenantResolutionException("Studio ID could not be resolved from authentication context");
        }
        return studioId;
    }
}
