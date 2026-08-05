package br.com.corely.auth.security;

import br.com.corely.auth.security.ratelimit.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolve o endereço IP do cliente (EPIC-02-S09).
 *
 * <p>Por padrão usa {@code remoteAddr}. Se {@code corely.rate-limit.trust-forwarded-header}
 * estiver habilitado (exigindo proxy de ingresso confiável que sobrescreva o
 * header), considera o primeiro valor de {@code X-Forwarded-For}. A lógica é
 * compartilhada entre o {@code RateLimitFilter} e a trilha de auditoria.</p>
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final RateLimitProperties rateLimitProperties;

    /**
     * Resolve o IP da requisição atual.
     *
     * @param request requisição HTTP
     * @return IP do cliente
     */
    public String resolve(HttpServletRequest request) {
        if (rateLimitProperties.isTrustForwardedHeader()) {
            String forwarded = request.getHeader(X_FORWARDED_FOR);
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Resolve o IP da requisição corrente do thread (via RequestContextHolder).
     *
     * @return IP do cliente ou {@code null} fora de um contexto de requisição
     */
    public String resolveCurrentRequestIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return resolve(attributes.getRequest());
        }
        return null;
    }
}
