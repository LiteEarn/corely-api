package br.com.corely.finance.situation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários da derivação de situação financeira (EPIC-03-S03).
 */
class SituationTest {

    @Test
    void from_shouldReturnOpenWhenNotDueAndNotPaid() {
        assertThat(Situation.from("OPEN", LocalDate.now().plusDays(5))).isEqualTo(Situation.OPEN);
    }

    @Test
    void from_shouldReturnOverdueWhenOpenAndDueDateInPast() {
        assertThat(Situation.from("OPEN", LocalDate.now().minusDays(1))).isEqualTo(Situation.OVERDUE);
    }

    @Test
    void from_shouldReturnPaidForPaidStatus() {
        assertThat(Situation.from("PAID", LocalDate.now().minusDays(10))).isEqualTo(Situation.PAID);
    }

    @Test
    void from_shouldReturnReversedForCancelledStatus() {
        assertThat(Situation.from("CANCELLED", LocalDate.now())).isEqualTo(Situation.REVERSED);
    }

    @Test
    void from_shouldReturnOpenWhenDueDateIsToday() {
        assertThat(Situation.from("OPEN", LocalDate.now())).isEqualTo(Situation.OPEN);
    }

    @Test
    void from_shouldTreatNullDueDateAsOpen() {
        assertThat(Situation.from("OPEN", null)).isEqualTo(Situation.OPEN);
    }
}
