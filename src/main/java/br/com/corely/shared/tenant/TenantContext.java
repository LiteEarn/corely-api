package br.com.corely.shared.tenant;

import br.com.corely.auth.security.AuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Contexto global de tenant do Corely.
 *
 * <p>Resolve o {@code studioId} corrente exclusivamente a partir do contexto de
 * autenticação da requisição — nunca de parâmetros controlados pelo client.
 * É a fonte única de resolução de tenant reutilizada pelos filtros de
 * isolamento (ver {@code TenantInterceptor}) e pelos Services.</p>
 */
@Component
@RequiredArgsConstructor
public class TenantContext {

    private final AuthenticationFacade authenticationFacade;

    public UUID getCurrentStudioId() {
        UUID studioId = authenticationFacade.getCurrentStudioId();
        if (studioId == null) {
            throw new TenantResolutionException("Studio ID could not be resolved from authentication context");
        }
        return studioId;
    }

    public UUID getCurrentUserId() {
        return authenticationFacade.getCurrentUserId();
    }
}
