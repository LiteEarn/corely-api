package br.com.corely.comercial.plan;

import br.com.corely.comercial.plan.dto.PlanRequest;
import br.com.corely.comercial.plan.dto.PlanResponse;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;
    @Mock
    private StudioRepository studioRepository;
    @Mock
    private ComercialTenantContext tenantContext;
    @Mock
    private StudentPlanRepository studentPlanRepository;
    @Mock
    private PlanRuleRepository planRuleRepository;

    private PlanService planService;

    private UUID studioId;
    private Studio studio;
    private Plan activePlan;
    private UUID activePlanId;

    @BeforeEach
    void setUp() {
        planService = new PlanService(planRepository, studioRepository, tenantContext,
                studentPlanRepository, planRuleRepository);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);
        studio.setName("Test Studio");

        activePlanId = UUID.randomUUID();
        activePlan = new Plan();
        activePlan.setId(activePlanId);
        activePlan.setStudio(studio);
        activePlan.setName("Plano Mensal");
        activePlan.setDescription("Acesso ilimitado");
        activePlan.setPrice(BigDecimal.valueOf(250));
        activePlan.setDuration(1);
        activePlan.setVersion(1);
        activePlan.setActive(true);
        activePlan.setAutoRenew(true);
    }

    @Test
    void create_shouldSaveAndReturnResponse() {
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(planRepository.existsByStudioIdAndName(studioId, "Plano Mensal")).thenReturn(false);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> {
            Plan p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(studentPlanRepository.countByPlanIdAndStatus(any(), eq(StudentPlanStatus.ACTIVE))).thenReturn(0L);
        when(planRuleRepository.countByPlanId(any())).thenReturn(0L);

        var request = new PlanRequest();
        request.setName("Plano Mensal");
        request.setDescription("Acesso ilimitado");
        request.setPrice(BigDecimal.valueOf(250));
        request.setDuration(1);
        request.setAutoRenew(true);

        var response = planService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Plano Mensal");
        assertThat(response.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(250));
        assertThat(response.getActive()).isTrue();
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getActiveStudentCount()).isEqualTo(0L);
        assertThat(response.getRuleCount()).isEqualTo(0L);
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void create_shouldThrowWhenNameAlreadyExists() {
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(planRepository.existsByStudioIdAndName(studioId, "Plano Mensal")).thenReturn(true);

        var request = new PlanRequest();
        request.setName("Plano Mensal");
        request.setPrice(BigDecimal.valueOf(250));
        request.setDuration(1);

        assertThatThrownBy(() -> planService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Plan name already exists");
    }

    @Test
    void update_shouldSaveAndIncrementVersion() {
        when(planRepository.findById(activePlanId)).thenReturn(Optional.of(activePlan));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(planRepository.existsByStudioIdAndNameAndIdNot(studioId, "Plano Mensal", activePlanId)).thenReturn(false);
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentPlanRepository.countByPlanIdAndStatus(any(), eq(StudentPlanStatus.ACTIVE))).thenReturn(5L);
        when(planRuleRepository.countByPlanId(any())).thenReturn(3L);

        var request = new PlanRequest();
        request.setName("Plano Mensal");
        request.setPrice(BigDecimal.valueOf(299));
        request.setDuration(1);

        var response = planService.update(activePlanId, request);

        assertThat(response.getVersion()).isEqualTo(2);
        assertThat(response.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(299));
        assertThat(response.getActiveStudentCount()).isEqualTo(5L);
        assertThat(response.getRuleCount()).isEqualTo(3L);
    }

    @Test
    void update_shouldThrowWhenPlanNotFound() {
        when(planRepository.findById(any())).thenReturn(Optional.empty());

        var request = new PlanRequest();
        request.setName("Test");
        request.setPrice(BigDecimal.valueOf(100));
        request.setDuration(1);

        assertThatThrownBy(() -> planService.update(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found");
    }

    @Test
    void inactivate_shouldDeactivatePlan() {
        when(planRepository.findById(activePlanId)).thenReturn(Optional.of(activePlan));
        when(studentPlanRepository.existsByContractSnapshotPlanIdAndStatus(activePlanId, StudentPlanStatus.ACTIVE))
                .thenReturn(false);
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        planService.inactivate(activePlanId);

        assertThat(activePlan.getActive()).isFalse();
        assertThat(activePlan.getVersion()).isEqualTo(2);
    }

    @Test
    void inactivate_shouldThrowWhenPlanAlreadyInactive() {
        activePlan.setActive(false);
        when(planRepository.findById(activePlanId)).thenReturn(Optional.of(activePlan));

        assertThatThrownBy(() -> planService.inactivate(activePlanId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Plan is already inactive");
    }

    @Test
    void inactivate_shouldThrowWhenHasActiveContracts() {
        when(planRepository.findById(activePlanId)).thenReturn(Optional.of(activePlan));
        when(studentPlanRepository.existsByContractSnapshotPlanIdAndStatus(activePlanId, StudentPlanStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> planService.inactivate(activePlanId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot inactivate plan with active student contracts");
    }

    @Test
    void activate_shouldActivatePlan() {
        activePlan.setActive(false);
        when(planRepository.findById(activePlanId)).thenReturn(Optional.of(activePlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        planService.activate(activePlanId);

        assertThat(activePlan.getActive()).isTrue();
        assertThat(activePlan.getVersion()).isEqualTo(2);
    }

    @Test
    void activate_shouldThrowWhenPlanAlreadyActive() {
        when(planRepository.findById(activePlanId)).thenReturn(Optional.of(activePlan));

        assertThatThrownBy(() -> planService.activate(activePlanId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Plan is already active");
    }

    @Test
    void delete_shouldDeletePlanAndRules() {
        when(planRepository.findById(activePlanId)).thenReturn(Optional.of(activePlan));
        when(studentPlanRepository.existsByContractSnapshotPlanIdAndStatus(activePlanId, StudentPlanStatus.ACTIVE))
                .thenReturn(false);
        when(studentPlanRepository.existsByContractSnapshotPlanIdAndStatus(activePlanId, StudentPlanStatus.SUSPENDED))
                .thenReturn(false);
        when(planRuleRepository.countByPlanId(activePlanId)).thenReturn(2L);
        when(planRuleRepository.findByPlanIdOrderByCreatedAt(activePlanId)).thenReturn(List.of());

        planService.delete(activePlanId);

        verify(planRuleRepository).deleteAll(any());
        verify(planRepository).delete(activePlan);
    }

    @Test
    void delete_shouldDeletePlanWithoutRules() {
        when(planRepository.findById(activePlanId)).thenReturn(Optional.of(activePlan));
        when(studentPlanRepository.existsByContractSnapshotPlanIdAndStatus(activePlanId, StudentPlanStatus.ACTIVE))
                .thenReturn(false);
        when(studentPlanRepository.existsByContractSnapshotPlanIdAndStatus(activePlanId, StudentPlanStatus.SUSPENDED))
                .thenReturn(false);
        when(planRuleRepository.countByPlanId(activePlanId)).thenReturn(0L);

        planService.delete(activePlanId);

        verify(planRuleRepository, never()).deleteAll(any());
        verify(planRepository).delete(activePlan);
    }

    @Test
    void delete_shouldThrowWhenHasActiveContracts() {
        when(planRepository.findById(activePlanId)).thenReturn(Optional.of(activePlan));
        when(studentPlanRepository.existsByContractSnapshotPlanIdAndStatus(activePlanId, StudentPlanStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> planService.delete(activePlanId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete plan with active or suspended student contracts");
    }

    @Test
    void delete_shouldThrowWhenHasSuspendedContracts() {
        when(planRepository.findById(activePlanId)).thenReturn(Optional.of(activePlan));
        when(studentPlanRepository.existsByContractSnapshotPlanIdAndStatus(activePlanId, StudentPlanStatus.ACTIVE))
                .thenReturn(false);
        when(studentPlanRepository.existsByContractSnapshotPlanIdAndStatus(activePlanId, StudentPlanStatus.SUSPENDED))
                .thenReturn(true);

        assertThatThrownBy(() -> planService.delete(activePlanId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete plan with active or suspended student contracts");
    }

    @Test
    void delete_shouldThrowWhenPlanNotFound() {
        when(planRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.delete(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found");
    }

    @Test
    void findAll_shouldReturnPageWithCounts() {
        var page = new PageImpl<>(List.of(activePlan), PageRequest.of(0, 10), 1);
        when(planRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);
        when(studentPlanRepository.countByPlanIdAndStatus(any(), eq(StudentPlanStatus.ACTIVE))).thenReturn(3L);
        when(planRuleRepository.countByPlanId(any())).thenReturn(2L);

        var result = planService.findAll(null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getActiveStudentCount()).isEqualTo(3L);
        assertThat(result.getContent().get(0).getRuleCount()).isEqualTo(2L);
    }

    @Test
    void findById_shouldReturnPlanWithCounts() {
        when(planRepository.findById(activePlanId)).thenReturn(Optional.of(activePlan));
        when(studentPlanRepository.countByPlanIdAndStatus(activePlanId, StudentPlanStatus.ACTIVE)).thenReturn(7L);
        when(planRuleRepository.countByPlanId(activePlanId)).thenReturn(4L);

        var response = planService.findById(activePlanId);

        assertThat(response.getId()).isEqualTo(activePlanId);
        assertThat(response.getActiveStudentCount()).isEqualTo(7L);
        assertThat(response.getRuleCount()).isEqualTo(4L);
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        when(planRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Plan not found");
    }
}
