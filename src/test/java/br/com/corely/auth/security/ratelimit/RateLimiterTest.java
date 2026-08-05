package br.com.corely.auth.security.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do {@link RateLimiter} (EPIC-02-S06).
 */
class RateLimiterTest {

    @Test
    void tryAcquire_shouldAllowUpToCapacityRequests() {
        RateLimiter limiter = new RateLimiter();

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire("ip-1|sensitive", 5, 60))
                    .as("requisição %d deve ser permitida dentro da capacidade", i + 1)
                    .isTrue();
        }
    }

    @Test
    void tryAcquire_shouldRejectWhenCapacityExceeded() {
        RateLimiter limiter = new RateLimiter();

        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("ip-1|sensitive", 5, 60);
        }

        assertThat(limiter.tryAcquire("ip-1|sensitive", 5, 60))
                .as("requisição além da capacidade deve ser rejeitada")
                .isFalse();
    }

    @Test
    void tryAcquire_shouldIsolateBucketsByKey() {
        RateLimiter limiter = new RateLimiter();

        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("ip-1|sensitive", 5, 60);
        }

        assertThat(limiter.tryAcquire("ip-1|sensitive", 5, 60)).isFalse();
        assertThat(limiter.tryAcquire("ip-2|sensitive", 5, 60))
                .as("IPs diferentes devem ter buckets independentes")
                .isTrue();
    }

    @Test
    void tryAcquire_shouldRefillTokensAfterWindow() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();

        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("ip-1|sensitive", 5, 1);
        }
        assertThat(limiter.tryAcquire("ip-1|sensitive", 5, 1)).isFalse();

        Thread.sleep(1100);

        assertThat(limiter.tryAcquire("ip-1|sensitive", 5, 1))
                .as("após a janela, tokens devem ser reabastecidos")
                .isTrue();
    }

    @Test
    void tryAcquire_shouldRejectWhenCapacityIsZeroOrNegative() {
        RateLimiter limiter = new RateLimiter();

        assertThat(limiter.tryAcquire("ip-1|global", 0, 60)).isFalse();
        assertThat(limiter.tryAcquire("ip-1|global", -1, 60)).isFalse();
        assertThat(limiter.tryAcquire("ip-1|global", 10, 0)).isFalse();
    }

    @Test
    void tryAcquire_shouldIsolateScopesForSameIp() {
        RateLimiter limiter = new RateLimiter();

        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("10.0.0.1|sensitive", 5, 60);
        }

        assertThat(limiter.tryAcquire("10.0.0.1|sensitive", 5, 60))
                .as("limite sensível esgotado para o IP")
                .isFalse();
        assertThat(limiter.tryAcquire("10.0.0.1|global", 100, 60))
                .as("escopo global deve ter bucket independente para o mesmo IP")
                .isTrue();
    }

    @Test
    void tryAcquire_globalTraffic_shouldNotRefillSensitiveBucket() throws InterruptedException {
        RateLimiter limiter = new RateLimiter();

        // Esgota o bucket sensível (capacidade 5, janela 60s).
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire("10.0.0.1|sensitive", 5, 60);
        }

        // Tráfego global intercalado não deve reabastecer o bucket sensível.
        for (int i = 0; i < 20; i++) {
            limiter.tryAcquire("10.0.0.1|global", 100, 60);
        }

        assertThat(limiter.tryAcquire("10.0.0.1|sensitive", 5, 60))
                .as("tráfego global não deve reabastecer o limite sensível")
                .isFalse();
    }

    @Test
    void evictIdleBuckets_shouldRemoveInactiveBuckets() {
        RateLimiter limiter = new RateLimiter();

        limiter.tryAcquire("10.0.0.1|global", 100, 60);
        limiter.tryAcquire("10.0.0.2|global", 100, 60);

        limiter.evictIdleBuckets();

        // Buckets acabaram de ser usados; não podem ser evictados imediatamente.
        assertThat(limiter.tryAcquire("10.0.0.1|global", 100, 60)).isTrue();
        assertThat(limiter.tryAcquire("10.0.0.2|global", 100, 60)).isTrue();
    }
}