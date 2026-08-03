package br.com.corely.auth.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Propriedades de configuração do JWT.
 *
 * <p>Suporta rotação de segredo sem downtime: o segredo <b>atual</b>
 * ({@link #secret}) assina novos tokens, enquanto os segredos <b>anteriores</b>
 * ({@link #previousSecrets}) permanecem aceitos para validação de tokens já
 * emitidos antes da rotação (EPIC-02-S02).</p>
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;

    private List<String> previousSecrets = List.of();

    private long accessTokenExpiration = 900000L;

    private long refreshTokenExpiration = 604800000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<String> getPreviousSecrets() {
        return previousSecrets;
    }

    public void setPreviousSecrets(List<String> previousSecrets) {
        this.previousSecrets = previousSecrets == null ? List.of() : previousSecrets;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(long accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(long refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }
}
