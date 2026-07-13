package br.com.corely.comercial.ruleengine;

import br.com.corely.comercial.ruledefinition.ValueType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleResolverTest {

    @Test
    void resolve_shouldConvertBoolean() {
        assertThat(RuleResolver.resolve("true", ValueType.BOOLEAN)).isEqualTo(true);
        assertThat(RuleResolver.resolve("false", ValueType.BOOLEAN)).isEqualTo(false);
    }

    @Test
    void resolve_shouldConvertInteger() {
        assertThat(RuleResolver.resolve("42", ValueType.INTEGER)).isEqualTo(42);
        assertThat(RuleResolver.resolve("0", ValueType.INTEGER)).isEqualTo(0);
        assertThat(RuleResolver.resolve("-5", ValueType.INTEGER)).isEqualTo(-5);
    }

    @Test
    void resolve_shouldConvertDecimal() {
        assertThat(RuleResolver.resolve("15.50", ValueType.DECIMAL)).isEqualTo(new BigDecimal("15.50"));
        assertThat(RuleResolver.resolve("0", ValueType.DECIMAL)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void resolve_shouldKeepString() {
        assertThat(RuleResolver.resolve("MONTHLY", ValueType.STRING)).isEqualTo("MONTHLY");
        assertThat(RuleResolver.resolve("any value", ValueType.STRING)).isEqualTo("any value");
    }

    @Test
    void resolve_shouldKeepEnum() {
        assertThat(RuleResolver.resolve("PLATINUM", ValueType.ENUM)).isEqualTo("PLATINUM");
    }

    @Test
    void resolve_shouldReturnNull_whenValueIsNull() {
        assertThat(RuleResolver.resolve(null, ValueType.STRING)).isNull();
        assertThat(RuleResolver.resolve(null, ValueType.INTEGER)).isNull();
        assertThat(RuleResolver.resolve(null, ValueType.BOOLEAN)).isNull();
    }

    @Test
    void resolve_shouldThrowException_whenInvalidInteger() {
        assertThatThrownBy(() -> RuleResolver.resolve("not-a-number", ValueType.INTEGER))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void resolve_shouldThrowException_whenInvalidDecimal() {
        assertThatThrownBy(() -> RuleResolver.resolve("not-a-decimal", ValueType.DECIMAL))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void resolve_shouldReturnFalse_whenInvalidBoolean() {
        assertThat((Boolean) RuleResolver.resolve("not-boolean", ValueType.BOOLEAN)).isFalse();
        assertThat((Boolean) RuleResolver.resolve("yes", ValueType.BOOLEAN)).isFalse();
        assertThat((Boolean) RuleResolver.resolve("1", ValueType.BOOLEAN)).isFalse();
    }
}
