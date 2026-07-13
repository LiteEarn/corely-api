package br.com.corely.comercial.contractsnapshot;

import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.ruleengine.RuleEngine;
import br.com.corely.comercial.ruleengine.RuleResult;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractSnapshotServiceTest {

    @Mock
    private ContractSnapshotRepository repository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private RuleEngine ruleEngine;

    private ObjectMapper objectMapper;

    private ContractSnapshotService service;

    private Plan plan;
    private UUID planId;
    private UUID studioId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ContractSnapshotService(repository, planRepository, ruleEngine, objectMapper);

        studioId = UUID.randomUUID();
        planId = UUID.randomUUID();

        Studio studio = new Studio();
        studio.setId(studioId);
        studio.setName("Studio");
        studio.setActive(true);

        plan = new Plan();
        plan.setId(planId);
        plan.setStudio(studio);
        plan.setName("Standard Plan");
        plan.setDescription("A standard plan");
        plan.setPrice(BigDecimal.valueOf(99.90));
        plan.setDuration(30);
        plan.setVersion(3);
    }

    @Test
    void create_shouldPersistSnapshotWithPlanData() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        var rulesMap = new LinkedHashMap<String, Object>();
        rulesMap.put("VALIDITY_DAYS", 30);
        rulesMap.put("AUTO_RENEW", true);
        rulesMap.put("BILLING_CYCLE", "MONTHLY");
        when(ruleEngine.evaluate(plan)).thenReturn(new RuleResult(rulesMap));

        when(repository.save(any(ContractSnapshot.class))).thenAnswer(invocation -> {
            ContractSnapshot cs = invocation.getArgument(0);
            cs.setId(UUID.randomUUID());
            return cs;
        });

        ContractSnapshot result = service.create(planId);

        assertThat(result.getStudioId()).isEqualTo(studioId);
        assertThat(result.getPlanId()).isEqualTo(planId);
        assertThat(result.getPlanVersion()).isEqualTo(3);
        assertThat(result.getPlanName()).isEqualTo("Standard Plan");
        assertThat(result.getPlanDescription()).isEqualTo("A standard plan");
        assertThat(result.getPlanPrice()).isEqualByComparingTo(new BigDecimal("99.90"));
        assertThat(result.getPlanDuration()).isEqualTo(30);
        assertThat(result.getRules()).isEqualTo("{\"VALIDITY_DAYS\":30,\"AUTO_RENEW\":true,\"BILLING_CYCLE\":\"MONTHLY\"}");
    }

    @Test
    void create_shouldPersistSnapshotWithEmptyRules() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(ruleEngine.evaluate(plan)).thenReturn(new RuleResult(new LinkedHashMap<>()));

        when(repository.save(any(ContractSnapshot.class))).thenAnswer(invocation -> {
            ContractSnapshot cs = invocation.getArgument(0);
            cs.setId(UUID.randomUUID());
            return cs;
        });

        ContractSnapshot result = service.create(planId);

        assertThat(result.getStudioId()).isEqualTo(studioId);
        assertThat(result.getPlanName()).isEqualTo("Standard Plan");
        assertThat(result.getRules()).isEqualTo("{}");
    }

    @Test
    void create_shouldThrowException_whenPlanNotFound() {
        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(planId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found");
    }

    @Test
    void create_shouldCapturePlanVersionAtMoment() {
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(ruleEngine.evaluate(plan)).thenReturn(new RuleResult(new LinkedHashMap<>(Map.of("MAX_CLASSES", 10))));

        plan.setVersion(5);

        when(repository.save(any(ContractSnapshot.class))).thenAnswer(invocation -> {
            ContractSnapshot cs = invocation.getArgument(0);
            cs.setId(UUID.randomUUID());
            return cs;
        });

        ContractSnapshot result = service.create(planId);

        assertThat(result.getPlanVersion()).isEqualTo(5);
        assertThat(result.getStudioId()).isEqualTo(studioId);
    }

    @Test
    void create_shouldHandleNullDescription() {
        plan.setDescription(null);

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(ruleEngine.evaluate(plan)).thenReturn(new RuleResult(new LinkedHashMap<>()));

        when(repository.save(any(ContractSnapshot.class))).thenAnswer(invocation -> {
            ContractSnapshot cs = invocation.getArgument(0);
            cs.setId(UUID.randomUUID());
            return cs;
        });

        ContractSnapshot result = service.create(planId);

        assertThat(result.getStudioId()).isEqualTo(studioId);
        assertThat(result.getPlanDescription()).isNull();
        assertThat(result.getRules()).isEqualTo("{}");
    }
}
