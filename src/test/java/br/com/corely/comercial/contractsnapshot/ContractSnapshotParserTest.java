package br.com.corely.comercial.contractsnapshot;

import br.com.corely.comercial.billingschedule.BillingFrequency;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractSnapshotParserTest {

    private final ContractSnapshotParser parser = new ContractSnapshotParser(new ObjectMapper());

    @Test
    void parse_shouldReturnEmpty_whenNullRules() {
        var result = parser.parse((String) null);

        assertThat(result.weeklyClasses()).isEmpty();
        assertThat(result.billingCycle()).isEmpty();
        assertThat(result.validityDays()).isEmpty();
        assertThat(result.autoRenew()).isEmpty();
    }

    @Test
    void parse_shouldReturnEmpty_whenBlankRules() {
        var result = parser.parse("   ");

        assertThat(result.weeklyClasses()).isEmpty();
        assertThat(result.billingCycle()).isEmpty();
    }

    @Test
    void parse_shouldReturnEmpty_whenEmptyJsonObject() {
        var result = parser.parse("{}");

        assertThat(result.weeklyClasses()).isEmpty();
        assertThat(result.billingCycle()).isEmpty();
    }

    @Test
    void parse_shouldReturnEmpty_whenInvalidJson() {
        var result = parser.parse("{invalid json");

        assertThat(result.weeklyClasses()).isEmpty();
        assertThat(result.billingCycle()).isEmpty();
    }

    @Test
    void parse_shouldReturnEmpty_whenSnapshotHasNullRules() {
        var snapshot = new ContractSnapshot();
        snapshot.setRules(null);

        var result = parser.parse(snapshot);

        assertThat(result.weeklyClasses()).isEmpty();
    }

    @Test
    void parse_shouldExtractWeeklyClasses_whenPresent() {
        var result = parser.parse("{\"WEEKLY_CLASSES\":3}");

        assertThat(result.weeklyClasses()).contains(3);
    }

    @Test
    void parse_shouldReturnEmptyWeeklyClasses_whenAbsent() {
        var result = parser.parse("{\"VALIDITY_DAYS\":30}");

        assertThat(result.weeklyClasses()).isEmpty();
    }

    @Test
    void parse_shouldReturnEmptyWeeklyClasses_whenValueNotNumeric() {
        var result = parser.parse("{\"WEEKLY_CLASSES\":\"not-a-number\"}");

        assertThat(result.weeklyClasses()).isEmpty();
    }

    @Test
    void parse_shouldReturnEmptyBillingCycle_whenAbsent() {
        var result = parser.parse("{\"VALIDITY_DAYS\":30}");

        assertThat(result.billingCycle()).isEmpty();
    }

    @Test
    void parse_shouldReturnEmptyBillingCycle_whenInvalidValue() {
        var result = parser.parse("{\"BILLING_CYCLE\":\"UNKNOWN\"}");

        assertThat(result.billingCycle()).isEmpty();
    }

    @Test
    void parse_shouldExtractBillingCycle_whenPresent() {
        var result = parser.parse("{\"BILLING_CYCLE\":\"MONTHLY\"}");

        assertThat(result.billingCycle()).contains(BillingFrequency.MONTHLY);
    }

    @Test
    void parse_shouldExtractValidityDays() {
        var result = parser.parse("{\"VALIDITY_DAYS\":45}");

        assertThat(result.validityDays()).contains(45);
    }

    @Test
    void parse_shouldExtractAutoRenew() {
        var result = parser.parse("{\"AUTO_RENEW\":true}");

        assertThat(result.autoRenew()).contains(true);
    }

    @Test
    void parse_shouldSupportNestedRulesSchema() {
        var result = parser.parse("{\"rules\":{\"weeklyClasses\":5}}");

        assertThat(result.weeklyClasses()).contains(5);
    }

    @Test
    void parse_shouldSupportCamelCaseKeys() {
        var result = parser.parse("{\"weeklyClasses\":2,\"billingCycle\":\"WEEKLY\"}");

        assertThat(result.weeklyClasses()).contains(2);
        assertThat(result.billingCycle()).contains(BillingFrequency.WEEKLY);
    }

    @Test
    void parse_shouldSupportMixedNestedAndFlatSchema() {
        var result = parser.parse("{\"rules\":{\"weeklyClasses\":4,\"billingCycle\":\"QUARTERLY\"}}");

        assertThat(result.weeklyClasses()).contains(4);
        assertThat(result.billingCycle()).contains(BillingFrequency.QUARTERLY);
    }
}
