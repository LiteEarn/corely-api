package br.com.corely.auth.security.lockout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do {@link LoginAttemptTracker} (EPIC-02-S07).
 */
class LoginAttemptTrackerTest {

    private LoginAttemptTracker tracker;
    private LoginLockoutProperties properties;

    @BeforeEach
    void setUp() {
        properties = new LoginLockoutProperties();
        tracker = new LoginAttemptTracker(properties);
    }

    @Test
    void recordFailure_shouldLockAfterMaxAttempts() {
        properties.setMaxAttempts(3);
        properties.setLockoutSeconds(900);

        assertThat(tracker.recordFailure("user@test.com")).isFalse();
        assertThat(tracker.recordFailure("user@test.com")).isFalse();
        assertThat(tracker.isLocked("user@test.com")).isFalse();

        assertThat(tracker.recordFailure("user@test.com"))
                .as("terceira falha deve bloquear o e-mail")
                .isTrue();
        assertThat(tracker.isLocked("user@test.com")).isTrue();
        assertThat(tracker.getRemainingLockoutSeconds("user@test.com")).isPositive();
    }

    @Test
    void recordFailure_shouldNotLockBelowMaxAttempts() {
        properties.setMaxAttempts(5);

        for (int i = 0; i < 4; i++) {
            tracker.recordFailure("user@test.com");
        }

        assertThat(tracker.isLocked("user@test.com")).isFalse();
    }

    @Test
    void reset_shouldClearFailures() {
        properties.setMaxAttempts(2);
        tracker.recordFailure("user@test.com");

        tracker.reset("user@test.com");

        assertThat(tracker.isLocked("user@test.com")).isFalse();
        assertThat(tracker.recordFailure("user@test.com")).isFalse();
        assertThat(tracker.isLocked("user@test.com"))
                .as("após reset, a contagem deve recomeçar")
                .isFalse();
    }

    @Test
    void recordFailure_shouldNormalizeEmailCase() {
        properties.setMaxAttempts(1);

        assertThat(tracker.recordFailure("User@Test.COM"))
                .as("e-mail deve ser normalizado em minúsculas")
                .isTrue();
        assertThat(tracker.isLocked("user@test.com"))
                .as("e-mails com caixa diferente devem compartilhar o rastreamento")
                .isTrue();
    }

    @Test
    void isLocked_shouldReturnFalseWhenDisabled() {
        properties.setEnabled(false);
        properties.setMaxAttempts(1);

        tracker.recordFailure("user@test.com");

        assertThat(tracker.isLocked("user@test.com")).isFalse();
    }

    @Test
    void getRemainingLockoutSeconds_shouldReturnZeroWhenNotLocked() {
        assertThat(tracker.getRemainingLockoutSeconds("user@test.com")).isZero();
        assertThat(tracker.isLocked("user@test.com")).isFalse();
    }

    @Test
    void isLocked_shouldExpireAfterWindow() throws InterruptedException {
        properties.setMaxAttempts(1);
        properties.setLockoutSeconds(1);

        tracker.recordFailure("user@test.com");
        assertThat(tracker.isLocked("user@test.com")).isTrue();

        Thread.sleep(1100);

        assertThat(tracker.isLocked("user@test.com"))
                .as("após a janela de lockout, o e-mail deve ser liberado")
                .isFalse();
    }

    @Test
    void recordFailure_afterLockoutExpiry_shouldRestartCount() throws InterruptedException {
        properties.setMaxAttempts(2);
        properties.setLockoutSeconds(1);

        tracker.recordFailure("user@test.com");
        tracker.recordFailure("user@test.com");
        assertThat(tracker.isLocked("user@test.com")).isTrue();

        Thread.sleep(1100);

        // Nova tentativa após expirar: a contagem recomeça.
        assertThat(tracker.recordFailure("user@test.com"))
                .as("após expirar o lockout, a primeira falha não deve re-bloquear")
                .isFalse();
    }

    @Test
    void evictIdleEntries_shouldKeepRecentlyActiveEntries() {
        properties.setMaxAttempts(5);
        tracker.recordFailure("user@test.com");

        tracker.evictIdleEntries();

        assertThat(tracker.isLocked("user@test.com")).isFalse();
        // A entrada ainda existe (não foi evictada imediatamente).
        assertThat(tracker.recordFailure("user@test.com")).isFalse();
    }

    @Test
    void evictIdleEntries_shouldRemoveEntryWithExpiredLockout() throws Exception {
        properties.setMaxAttempts(1);
        properties.setLockoutSeconds(1);

        tracker.recordFailure("user@test.com");
        assertThat(tracker.isLocked("user@test.com")).isTrue();

        // Após a janela, o lockout expira.
        Thread.sleep(1100);
        assertThat(tracker.isLocked("user@test.com")).isFalse();

        // A entrada com lockout expirado deve ser elegível para evicção:
        // simulamos que a última falha é antiga.
        java.lang.reflect.Field attemptsField = LoginAttemptTracker.class.getDeclaredField("attempts");
        attemptsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentMap<String, ?> attempts =
                (java.util.concurrent.ConcurrentMap<String, ?>) attemptsField.get(tracker);
        assertThat(attempts).containsKey("user@test.com");

        Object attempt = attempts.get("user@test.com");
        java.lang.reflect.Field lastFailureField = attempt.getClass().getDeclaredField("lastFailureNanos");
        lastFailureField.setAccessible(true);
        lastFailureField.setLong(attempt, System.nanoTime() - 2_000_000_000_000L); // 2000s atrás

        tracker.evictIdleEntries();

        assertThat(attempts).as("entrada com lockout expirado deve ser evictada")
                .doesNotContainKey("user@test.com");
    }
}