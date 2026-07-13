package br.com.corely.comercial.ruledefinition;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RuleDefinitionSeedTest {

    @Autowired
    private RuleDefinitionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final List<String> EXPECTED_CODES = List.of(
            "VALIDITY_DAYS",
            "MAX_CLASSES",
            "MAX_FUTURE_BOOKINGS",
            "DAILY_LIMIT",
            "ALLOW_MAKEUP",
            "MAKEUP_VALIDITY_DAYS",
            "AUTO_RENEW",
            "BILLING_CYCLE",
            "GRACE_PERIOD_DAYS",
            "ACTIVE_ON_PAYMENT",
            "ALLOW_OVERDUE_BOOKING"
    );

    @Test
    @Sql("classpath:db/migration/V30__seed_comercial_rule_definitions.sql")
    void shouldCreateAllRuleDefinitions() {
        var rules = repository.findAll();

        assertThat(rules).hasSize(11);

        var codes = rules.stream().map(RuleDefinition::getCode).toList();
        assertThat(codes).containsExactlyInAnyOrderElementsOf(EXPECTED_CODES);
        assertThat(codes).doesNotHaveDuplicates();
    }

    @Test
    @Sql("classpath:db/migration/V30__seed_comercial_rule_definitions.sql")
    void shouldStartAllAsActive() {
        var rules = repository.findAll();

        assertThat(rules).allMatch(RuleDefinition::getActive);
    }

    @Test
    @Sql("classpath:db/migration/V30__seed_comercial_rule_definitions.sql")
    void shouldHaveRequiredTrueForAll() {
        var rules = repository.findAll();

        assertThat(rules).allMatch(RuleDefinition::getRequired);
    }

    @Test
    void shouldBeIdempotent() throws Exception {
        var resource = new ClassPathResource("db/migration/V30__seed_comercial_rule_definitions.sql");
        var sql = new String(resource.getInputStream().readAllBytes());

        jdbcTemplate.execute(sql);
        jdbcTemplate.execute(sql);

        var count = repository.count();
        assertThat(count).isEqualTo(11);

        var rules = repository.findAll();
        assertThat(rules.stream().map(RuleDefinition::getCode).toList())
                .doesNotHaveDuplicates();
    }

    @Test
    @Sql("classpath:db/migration/V30__seed_comercial_rule_definitions.sql")
    void shouldHaveCorrectValueTypes() {
        var rules = repository.findAll();

        assertThat(rules.stream().filter(r -> r.getValueType() == ValueType.INTEGER))
                .hasSize(6);
        assertThat(rules.stream().filter(r -> r.getValueType() == ValueType.BOOLEAN))
                .hasSize(4);
        assertThat(rules.stream().filter(r -> r.getValueType() == ValueType.STRING))
                .hasSize(1);
    }

    @Test
    @Sql("classpath:db/migration/V30__seed_comercial_rule_definitions.sql")
    void shouldHaveCorrectCategories() {
        var rules = repository.findAll();

        assertThat(rules.stream().filter(r -> r.getCategory() == Category.VALIDITY))
                .hasSize(1);
        assertThat(rules.stream().filter(r -> r.getCategory() == Category.ATTENDANCE))
                .hasSize(1);
        assertThat(rules.stream().filter(r -> r.getCategory() == Category.BOOKING))
                .hasSize(2);
        assertThat(rules.stream().filter(r -> r.getCategory() == Category.CANCELLATION))
                .hasSize(2);
        assertThat(rules.stream().filter(r -> r.getCategory() == Category.BILLING))
                .hasSize(3);
        assertThat(rules.stream().filter(r -> r.getCategory() == Category.GENERAL))
                .hasSize(2);
    }
}
