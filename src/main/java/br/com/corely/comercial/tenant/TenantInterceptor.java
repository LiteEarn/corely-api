package br.com.corely.comercial.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final ComercialTenantContext tenantContext;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (path.startsWith("/comercial") || path.startsWith("/finance")) {
            try {
                var studioId = tenantContext.getCurrentStudioId();
                var session = entityManager.unwrap(Session.class);
                var filter = session.getEnabledFilter("comercialTenantFilter");
                if (filter == null) {
                    session.enableFilter("comercialTenantFilter")
                            .setParameter("studioId", studioId);
                }
            } catch (TenantResolutionException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Tenant não identificado no contexto de autenticação");
            }
        }
        return true;
    }
}
