package br.com.corely.comercial.ruleengine;

import br.com.corely.comercial.ruledefinition.RuleDefinition;

import java.math.BigDecimal;
import java.util.*;

public class RuleResult {

    private final Map<String, Object> resolvedValues;
    private final Map<String, RuleDefinition> definitions;
    private final Set<String> missingRequired;

    RuleResult(Map<String, Object> resolvedValues, Map<String, RuleDefinition> definitions, Set<String> missingRequired) {
        this.resolvedValues = Collections.unmodifiableMap(new LinkedHashMap<>(resolvedValues));
        this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
        this.missingRequired = Collections.unmodifiableSet(new LinkedHashSet<>(missingRequired));
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
        RuleDefinition def = definitions.get(code);
        if (def == null) {
            throw new RuleException("Rule '" + code + "' not found");
        }
        if (missingRequired.contains(code)) {
            throw new RuleException("Required rule '" + code + "' is not configured for this plan");
        }
        return resolvedValues.get(code);
    }
}
