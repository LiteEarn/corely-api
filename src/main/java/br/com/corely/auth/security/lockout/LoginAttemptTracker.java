package br.com.corely.auth.security.lockout;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rastreia tentativas de login inválidas por e-mail (EPIC-02-S07).
 *
 * <p>Mantém, para cada e-mail (normalizado em minúsculas), a contagem de falhas
 * consecutivas e o instante da última falha. Quando a contagem atinge o limite
 * configurado, o e-mail fica bloqueado por uma janela. A estrutura é
 * thread-safe via {@link ConcurrentHashMap} e sincronização por entrada.</p>
 *
 * <p>Entradas inativas (sem falhas por um período prolongado) são removidas
 * periodicamente para evitar crescimento ilimitado do mapa.</p>
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptTracker {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptTracker.class);

    /**
     * Tempo sem atividade (em segundos) após o qual uma entrada é considerada
     * ociosa e elegível para evicção.
     */
    private static final long IDLE_EVICTION_SECONDS = 1800;

    private final ConcurrentMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    private final LoginLockoutProperties properties;

    /**
     * Verifica se o e-mail está temporariamente bloqueado.
     *
     * @param email e-mail do usuário (normalizado internamente)
     * @return {@code true} se bloqueado e ainda dentro da janela de lockout
     */
    public boolean isLocked(String email) {
        if (!properties.isEnabled()) {
            return false;
        }
        Attempt attempt = attempts.get(normalize(email));
        if (attempt == null) {
            return false;
        }
        long lockedUntil = attempt.getLockedUntilNanos();
        return lockedUntil > 0 && System.nanoTime() < lockedUntil;
    }

    /**
     * Tempo restante (em segundos) de bloqueio para o e-mail.
     *
     * @param email e-mail do usuário
     * @return segundos restantes; {@code 0} se não bloqueado
     */
    public int getRemainingLockoutSeconds(String email) {
        Attempt attempt = attempts.get(normalize(email));
        if (attempt == null) {
            return 0;
        }
        long lockedUntil = attempt.getLockedUntilNanos();
        if (lockedUntil <= 0) {
            return 0;
        }
        long remainingNanos = lockedUntil - System.nanoTime();
        if (remainingNanos <= 0) {
            return 0;
        }
        return (int) Math.ceil(remainingNanos / 1_000_000_000.0);
    }

    /**
     * Registra uma falha de login. Retorna {@code true} se, após esta falha, o
     * e-mail ficou bloqueado.
     *
     * @param email e-mail do usuário
     * @return {@code true} se o e-mail ficou bloqueado por esta falha
     */
    public boolean recordFailure(String email) {
        if (!properties.isEnabled()) {
            return false;
        }
        String key = normalize(email);
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        return attempt.recordFailure(properties.getMaxAttempts(), properties.getLockoutSeconds());
    }

    /**
     * Reseta as tentativas do e-mail (após login bem-sucedido).
     *
     * @param email e-mail do usuário
     */
    public void reset(String email) {
        attempts.remove(normalize(email));
    }

    /**
     * Remove entradas ociosas para limitar o crescimento do mapa.
     */
    @Scheduled(fixedDelay = 1_800_000)
    public void evictIdleEntries() {
        long cutoff = System.nanoTime() - IDLE_EVICTION_SECONDS * 1_000_000_000L;
        int removed = 0;
        for (var entry : attempts.entrySet()) {
            Attempt attempt = entry.getValue();
            if (attempt.isIdleBefore(cutoff) && attempts.remove(entry.getKey(), attempt)) {
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Removidas {} entradas ociosas do tracker de login", removed);
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Controle de falhas e lockout de um e-mail.
     */
    private static final class Attempt {

        private int failures;
        private long lastFailureNanos;
        private long lockedUntilNanos;

        private synchronized boolean recordFailure(int maxAttempts, int lockoutSeconds) {
            long now = System.nanoTime();

            // Se o lockout anterior expirou, reinicia a contagem.
            if (lockedUntilNanos > 0 && now >= lockedUntilNanos) {
                failures = 0;
                lockedUntilNanos = 0;
            }

            failures++;
            lastFailureNanos = now;

            if (failures >= maxAttempts) {
                lockedUntilNanos = now + lockoutSeconds * 1_000_000_000L;
                failures = 0;
                return true;
            }
            return false;
        }

        private synchronized long getLockedUntilNanos() {
            return lockedUntilNanos;
        }

        private synchronized boolean isIdleBefore(long cutoffNanos) {
            boolean lockExpired = lockedUntilNanos <= 0 || System.nanoTime() >= lockedUntilNanos;
            return lockExpired && lastFailureNanos < cutoffNanos;
        }
    }
}