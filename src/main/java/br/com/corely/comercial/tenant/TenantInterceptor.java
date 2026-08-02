package br.com.corely.comercial.tenant;

import br.com.corely.shared.tenant.TenantContext;
import br.com.corely.shared.tenant.TenantResolutionException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Interceptor que habilita o filtro Hibernate {@code comercialTenantFilter} na
 * sessão da requisição, garantindo que toda consulta respeite o isolamento por
 * {@code studio_id}.
 *
 * <p>O filtro é habilitado para todas as rotas autenticadas que acessam dados de
 * tenant. Rotas públicas (autenticação, documentação, health check) e rotas
 * operacionais globais (seed {@code /dev}, scheduler administrativo {@code /admin})
 * são ignoradas por não possuírem contexto de tenant ou por operarem de forma
 * intencionalmente global.</p>
 */
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantContext tenantContext;

    @PersistenceContext
    private EntityManager entityManager;

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/auth", "/error", "/actuator", "/v3/api-docs", "/swagger-ui", "/dev", "/admin"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        try {
            var studioId = tenantContext.getCurrentStudioId();
            var session = entityManager.unwrap(Session.class);
            var filter = session.getEnabledFilter("comercialTenantFilter");
            if (filter == null) {
                session.enableFilter("comercialTenantFilter").setParameter("studioId", studioId);
            } else {
                filter.setParameter("studioId", studioId);
            }
        } catch (TenantResolutionException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Tenant não identificado no contexto de autenticação");
        }
        return true;
    }
}
