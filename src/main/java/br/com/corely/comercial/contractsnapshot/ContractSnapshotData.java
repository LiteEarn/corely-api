package br.com.corely.comercial.contractsnapshot;

import br.com.corely.comercial.billingschedule.BillingFrequency;

import java.util.Map;
import java.util.Optional;

public final class ContractSnapshotData {

    private final Integer weeklyClasses;
    private final BillingFrequency billingCycle;
    private final Integer validityDays;
    private final Boolean autoRenew;

    private ContractSnapshotData(Integer weeklyClasses, BillingFrequency billingCycle,
                                 Integer validityDays, Boolean autoRenew) {
        this.weeklyClasses = weeklyClasses;
        this.billingCycle = billingCycle;
        this.validityDays = validityDays;
        this.autoRenew = autoRenew;
    }

    static ContractSnapshotData empty() {
        return new ContractSnapshotData(null, null, null, null);
    }

    static ContractSnapshotData fromRules(Map<String, Object> rules) {
        Integer weeklyClasses = toInteger(rules.get("WEEKLYCLASSES"));
        BillingFrequency billingCycle = toFrequency(rules.get("BILLINGCYCLE"));
        Integer validityDays = toInteger(rules.get("VALIDITYDAYS"));
        Boolean autoRenew = toBoolean(rules.get("AUTORENEW"));
        return new ContractSnapshotData(weeklyClasses, billingCycle, validityDays, autoRenew);
    }

    public Optional<Integer> weeklyClasses() {
        return Optional.ofNullable(weeklyClasses);
    }

    public Optional<BillingFrequency> billingCycle() {
        return Optional.ofNullable(billingCycle);
    }

    public Optional<Integer> validityDays() {
        return Optional.ofNullable(validityDays);
    }

    public Optional<Boolean> autoRenew() {
        return Optional.ofNullable(autoRenew);
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String raw = value.toString().trim();
        if ("true".equalsIgnoreCase(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return false;
        }
        return null;
    }

    private static BillingFrequency toFrequency(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return BillingFrequency.valueOf(value.toString().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
