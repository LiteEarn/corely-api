package br.com.corely.auth.authorization;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    @Resource
    private AuthorizationService authorizationService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole != null) {
            boolean hasAccess = Arrays.stream(requireRole.value())
                    .anyMatch(role -> authorizationService.hasRole(role));
            if (!hasAccess) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: perfil requerido");
            }
        }

        RequireAnyRole requireAnyRole = handlerMethod.getMethodAnnotation(RequireAnyRole.class);
        if (requireAnyRole != null) {
            boolean hasAccess = Arrays.stream(requireAnyRole.value())
                    .anyMatch(role -> authorizationService.hasRole(role));
            if (!hasAccess) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: um dos perfis requeridos");
            }
        }

        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission != null) {
            boolean hasAccess = Arrays.stream(requirePermission.value())
                    .allMatch(permission -> authorizationService.hasPermission(permission));
            if (!hasAccess) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: permissão requerida");
            }
        }

        return true;
    }
}
