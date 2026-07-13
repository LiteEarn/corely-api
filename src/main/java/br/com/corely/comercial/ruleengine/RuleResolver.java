package br.com.corely.comercial.ruleengine;

import br.com.corely.comercial.ruledefinition.ValueType;

import java.math.BigDecimal;

public final class RuleResolver {

    private RuleResolver() {}

    public static Object resolve(String value, ValueType valueType) {
        if (value == null) {
            return null;
        }
        return switch (valueType) {
            case BOOLEAN -> Boolean.valueOf(value);
            case INTEGER -> Integer.valueOf(value);
            case DECIMAL -> new BigDecimal(value);
            case STRING -> value;
            case ENUM -> value;
        };
    }
}
