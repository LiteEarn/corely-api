package br.com.corely.auth.security.lockout;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de configuração do lockout de login (EPIC-02-S07).
 *
 * <p>Bloqueia temporariamente o login de um e-mail após um número de tentativas
 * inválidas consecutivas ({@link #maxAttempts}), por uma janela de
 * {@link #lockoutSeconds} segundos, mitigando brute force sobre a autenticação.
 * Pode ser desabilitado via {@code corely.login-lockout.enabled}.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "corely.login-lockout")
@Validated
public class LoginLockoutProperties {

    private boolean enabled = true;

    @Positive
    private int maxAttempts = 5;

    @Positive
    private int lockoutSeconds = 900;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getLockoutSeconds() {
        return lockoutSeconds;
    }

    public void setLockoutSeconds(int lockoutSeconds) {
        this.lockoutSeconds = lockoutSeconds;
    }
}