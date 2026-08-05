package br.com.corely.auth.security.lockout;

/**
 * Exceção lançada quando um e-mail está temporariamente bloqueado por excesso
 * de tentativas de login inválidas (EPIC-02-S07).
 *
 * <p>Indica ao cliente o tempo restante de bloqueio via {@link #getRetryAfterSeconds()}.</p>
 */
public class LoginLockoutException extends RuntimeException {

    private final int retryAfterSeconds;

    public LoginLockoutException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}