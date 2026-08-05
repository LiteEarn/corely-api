package br.com.corely.auth.security.ratelimit;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Propriedades de configuração do rate limiting (EPIC-02-S06).
 *
 * <p>O rate limiting é aplicado por chave (endereço IP do cliente) e pode ser
 * desabilitado via {@code corely.rate-limit.enabled}. Endpoints sensíveis
 * ({@link #sensitivePaths}) recebem um limite mais restrito
 * ({@link #sensitiveRequestsPerWindow}) para mitigar brute force em rotas como
 * autenticação.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "corely.rate-limit")
@Validated
public class RateLimitProperties {

    private boolean enabled = true;

    @Positive
    private int requestsPerWindow = 100;

    @Positive
    private int windowSeconds = 60;

    private List<String> sensitivePaths = List.of("/auth/**");

    @Positive
    private int sensitiveRequestsPerWindow = 5;

    /**
     * Controla se o IP do cliente é resolvido do header {@code X-Forwarded-For}.
     * <p>Habilitar exige que um proxy de ingresso confiável <b>sobrescreva</b>
     * o header (nunca apenas anexe) — caso contrário o cliente pode forjar o
     * valor e contornar o rate limit. Desabilitado por padrão (usa
     * {@code remoteAddr}).</p>
     */
    private boolean trustForwardedHeader = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRequestsPerWindow() {
        return requestsPerWindow;
    }

    public void setRequestsPerWindow(int requestsPerWindow) {
        this.requestsPerWindow = requestsPerWindow;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public List<String> getSensitivePaths() {
        return sensitivePaths;
    }

    public void setSensitivePaths(List<String> sensitivePaths) {
        this.sensitivePaths = sensitivePaths == null ? List.of() : sensitivePaths;
    }

    public int getSensitiveRequestsPerWindow() {
        return sensitiveRequestsPerWindow;
    }

    public void setSensitiveRequestsPerWindow(int sensitiveRequestsPerWindow) {
        this.sensitiveRequestsPerWindow = sensitiveRequestsPerWindow;
    }

    public boolean isTrustForwardedHeader() {
        return trustForwardedHeader;
    }

    public void setTrustForwardedHeader(boolean trustForwardedHeader) {
        this.trustForwardedHeader = trustForwardedHeader;
    }
}