package br.com.corely.comercial.contractsnapshot;

import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.*;
import br.com.corely.comercial.ruleengine.RuleEngine;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContractSnapshotIntegrationTest {

    @Autowired
    private ContractSnapshotService contractSnapshotService;

    @Autowired
    private ContractSnapshotRepository contractSnapshotRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanRuleRepository planRuleRepository;

    @Autowired
    private RuleDefinitionRepository ruleDefinitionRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RuleEngine ruleEngine;

    private Studio studio;
    private Plan plan;
    private RuleDefinition validityDays;
    private RuleDefinition autoRenew;
    private RuleDefinition billingCycle;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Snapshot Studio"));
        authenticateAs(studio, UserRole.OWNER);

        plan = planRepository.save(createPlan("Gold Plan", BigDecimal.valueOf(199), 30));

        validityDays = ruleDefinitionRepository.save(createRuleDef("VALIDITY_DAYS", ValueType.INTEGER));
        autoRenew = ruleDefinitionRepository.save(createRuleDef("AUTO_RENEW", ValueType.BOOLEAN));
        billingCycle = ruleDefinitionRepository.save(createRuleDef("BILLING_CYCLE", ValueType.STRING));
    }

    @Test
    void create_shouldPersistSnapshotWithPlanData() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));
        planRuleRepository.save(createPlanRule(plan, autoRenew, "true"));
        planRuleRepository.save(createPlanRule(plan, billingCycle, "MONTHLY"));

        ContractSnapshot snapshot = contractSnapshotService.create(plan.getId());

        assertThat(snapshot.getId()).isNotNull();
        assertThat(snapshot.getPlanId()).isEqualTo(plan.getId());
        assertThat(snapshot.getPlanVersion()).isEqualTo(1);
        assertThat(snapshot.getPlanName()).isEqualTo("Gold Plan");
        assertThat(snapshot.getPlanPrice()).isEqualByComparingTo(new BigDecimal("199"));
        assertThat(snapshot.getPlanDuration()).isEqualTo(30);
    }

    @Test
    void create_shouldPersistRulesAsJson() throws Exception {
        planRuleRepository.save(createPlanRule(plan, validityDays, "45"));
        planRuleRepository.save(createPlanRule(plan, autoRenew, "false"));

        ContractSnapshot snapshot = contractSnapshotService.create(plan.getId());

        Map<String, Object> rules = objectMapper.readValue(snapshot.getRules(), new TypeReference<>() {});
        assertThat(rules).hasSize(2);
        assertThat(rules).containsEntry("VALIDITY_DAYS", 45);
        assertThat(rules).containsEntry("AUTO_RENEW", false);
    }

    @Test
    void create_shouldCaptureCurrentPlanVersion() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        plan.setVersion(2);

        ContractSnapshot snapshot = contractSnapshotService.create(plan.getId());

        assertThat(snapshot.getPlanVersion()).isEqualTo(2);
    }

    @Test
    void create_shouldThrowException_whenPlanNotFound() {
        assertThatThrownBy(() -> contractSnapshotService.create(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found");
    }

    @Test
    void create_shouldCreateSnapshotWithEmptyRules_whenNoPlanRules() {
        ContractSnapshot snapshot = contractSnapshotService.create(plan.getId());

        assertThat(snapshot.getRules()).isEqualTo("{}");
    }

    @Test
    void create_shouldStoreSnapshotInDatabase() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        contractSnapshotService.create(plan.getId());

        assertThat(contractSnapshotRepository.count()).isEqualTo(1);
    }

    @Test
    void create_shouldAllowMultipleSnapshotsForSamePlan() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        contractSnapshotService.create(plan.getId());
        contractSnapshotService.create(plan.getId());

        assertThat(contractSnapshotRepository.count()).isEqualTo(2);
    }

    @Test
    void findByPlanId_shouldReturnSnapshotsInOrder() {
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        contractSnapshotService.create(plan.getId());

        var snapshots = contractSnapshotRepository.findByPlanIdOrderByCreatedAtDesc(plan.getId());

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).getPlanName()).isEqualTo("Gold Plan");
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "_" + studio.getId() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Studio createStudio(String name) {
        var studio = new Studio();
        studio.setName(name);
        studio.setActive(true);
        return studio;
    }

    private Plan createPlan(String name, BigDecimal price, Integer duration) {
        var plan = new Plan();
        plan.setStudio(studio);
        plan.setName(name);
        plan.setPrice(price);
        plan.setDuration(duration);
        plan.setVersion(1);
        plan.setActive(true);
        return plan;
    }

    private RuleDefinition createRuleDef(String code, ValueType valueType) {
        var rule = new RuleDefinition();
        rule.setCode(code);
        rule.setName(code);
        rule.setValueType(valueType);
        rule.setCategory(Category.GENERAL);
        rule.setActive(true);
        return rule;
    }

    private PlanRule createPlanRule(Plan plan, RuleDefinition ruleDef, String value) {
        var pr = new PlanRule();
        pr.setStudio(studio);
        pr.setPlan(plan);
        pr.setRuleDefinition(ruleDef);
        pr.setValue(value);
        return pr;
    }
}
