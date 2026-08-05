package br.com.corely.auth.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de rate limiting por endereço IP (EPIC-02-S06).
 *
 * <p>Aplica um limite global de requisições por janela e um limite mais
 * restrito para endpoints sensíveis (ex.: autenticação), mitigando brute force
 * e abuso. Os escopos global e sensível possuem buckets independentes por IP,
 * de modo que tráfego global não reabastece o limite sensível e o esgotamento
 * do limite sensível não bloqueia os demais endpoints. Quando o limite é
 * excedido, responde {@code 429 Too Many Requests} com o header
 * {@code Retry-After}. Pode ser desabilitado via
 * {@code corely.rate-limit.enabled}.</p>
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String SCOPE_SEPARATOR = "|";
    private static final String SCOPE_GLOBAL = "global";
    private static final String SCOPE_SENSITIVE = "sensitive";

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Preflight OPTIONS (CORS) é liberado na autorização; não deve consumir tokens.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();
        boolean sensitive = isSensitivePath(path);

        int capacity = sensitive ? properties.getSensitiveRequestsPerWindow() : properties.getRequestsPerWindow();
        int windowSeconds = properties.getWindowSeconds();
        String scope = sensitive ? SCOPE_SENSITIVE : SCOPE_GLOBAL;
        String key = clientIp + SCOPE_SEPARATOR + scope;

        if (!rateLimiter.tryAcquire(key, capacity, windowSeconds)) {
            log.debug("Rate limit excedido para IP {} em {} (scope={})", clientIp, path, scope);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSensitivePath(String path) {
        return properties.getSensitivePaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (properties.isTrustForwardedHeader()) {
            String forwarded = request.getHeader(X_FORWARDED_FOR);
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}