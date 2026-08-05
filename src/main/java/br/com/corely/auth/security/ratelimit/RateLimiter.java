package br.com.corely.auth.security.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate limiter in-memory baseado em token bucket (EPIC-02-S06).
 *
 * <p>Mantém um bucket por chave. Cada bucket possui uma capacidade máxima de
 * tokens e é reabastecido continuamente a uma taxa determinada pela janela
 * configurada no momento da criação — a capacidade e a janela são fixadas no
 * bucket, de modo que chaves diferentes possuem escopos independentes. A
 * estrutura é thread-safe via {@link ConcurrentHashMap} e sincronização por
 * bucket.</p>
 *
 * <p>Buckets inativos (sem requisição por um período prolongado) são removidos
 * periodicamente para evitar crescimento ilimitado do mapa.</p>
 */
@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    /**
     * Tempo sem atividade (em segundos) após o qual um bucket é considerado
     * ocioso e elegível para evicção.
     */
    private static final long IDLE_EVICTION_SECONDS = 300;

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Tenta consumir um token do bucket da chave informada.
     *
     * @param key            chave de rate limiting (ex.: IP do cliente + escopo)
     * @param capacity       capacidade máxima de tokens do bucket
     * @param windowSeconds  janela (em segundos) para reabastecimento completo
     * @return {@code true} se um token foi consumido; {@code false} se o limite foi excedido
     */
    public boolean tryAcquire(String key, int capacity, int windowSeconds) {
        if (capacity <= 0 || windowSeconds <= 0) {
            return false;
        }
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, windowSeconds));
        return bucket.tryAcquire();
    }

    /**
     * Remove buckets ociosos para limitar o crescimento do mapa.
     */
    @Scheduled(fixedDelay = 300_000)
    public void evictIdleBuckets() {
        long cutoff = System.nanoTime() - IDLE_EVICTION_SECONDS * 1_000_000_000L;
        int removed = 0;
        for (var entry : buckets.entrySet()) {
            Bucket bucket = entry.getValue();
            if (bucket.isIdleBefore(cutoff) && buckets.remove(entry.getKey(), bucket)) {
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Removidos {} buckets ociosos do rate limiter", removed);
        }
    }

    /**
     * Bucket de tokens com reabastecimento contínuo. Capacidade e janela são
     * fixadas na criação, garantindo escopo independente por chave.
     */
    private static final class Bucket {

        private final int capacity;
        private final double refillPerSecond;
        private double tokens;
        private long lastRefillNanos;

        private Bucket(int capacity, int windowSeconds) {
            this.capacity = capacity;
            this.refillPerSecond = (double) capacity / windowSeconds;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        private synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
            lastRefillNanos = now;

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private synchronized boolean isIdleBefore(long cutoffNanos) {
            return lastRefillNanos < cutoffNanos;
        }
    }
}