package br.com.corely.comercial.tenant;

import br.com.corely.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Contexto de tenant do módulo Comercial.
 *
 * <p>Delega para o contexto global {@link TenantContext} — fonte única de
 * resolução de {@code studioId} a partir do contexto de autenticação. Mantido
 * como compatibilidade para os serviços do módulo Comercial; novos módulos
 * devem injetar {@link TenantContext} diretamente.</p>
 */
@Component
@RequiredArgsConstructor
public class ComercialTenantContext {

    private final TenantContext tenantContext;

    public UUID getCurrentStudioId() {
        return tenantContext.getCurrentStudioId();
    }

    public UUID getCurrentUserId() {
        return tenantContext.getCurrentUserId();
    }
}
