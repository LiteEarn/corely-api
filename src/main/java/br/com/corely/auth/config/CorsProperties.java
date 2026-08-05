package br.com.corely.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Propriedades de configuração de CORS (EPIC-02-S08).
 *
 * <p>Define as origens permitidas por ambiente via {@code corely.cors.allowed-origins}.
 * No profile {@code prod} as origens devem ser resolvidas de variável de
 * ambiente ({@code CORS_ALLOWED_ORIGINS}), restringindo o CORS à origem real do
 * frontend em produção.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "corely.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
    }
}