package br.com.corely.comercial.ruleengine;

import java.math.BigDecimal;
import java.util.*;

public class RuleResult {

    private final Map<String, Object> resolvedValues;

    RuleResult(Map<String, Object> resolvedValues) {
        this.resolvedValues = Collections.unmodifiableMap(new LinkedHashMap<>(resolvedValues));
    }

    public Integer getInteger(String code) {
        Object value = getValueOrThrow(code);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        throw new RuleException("Value for rule '" + code + "' is not an Integer");
    }

    public Boolean getBoolean(String code) {
        Object value = getValueOrThrow(code);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        throw new RuleException("Value for rule '" + code + "' is not a Boolean");
    }

    public String getString(String code) {
        Object value = getValueOrThrow(code);
        if (value == null) return null;
        return value.toString();
    }

    public BigDecimal getDecimal(String code) {
        Object value = getValueOrThrow(code);
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        throw new RuleException("Value for rule '" + code + "' is not a Decimal");
    }

    public Set<String> getCodes() {
        return resolvedValues.keySet();
    }

    public boolean hasCode(String code) {
        return resolvedValues.containsKey(code);
    }

    private Object getValueOrThrow(String code) {
        if (!resolvedValues.containsKey(code)) {
            throw new RuleException("Rule '" + code + "' not found");
        }
        return resolvedValues.get(code);
    }
}
