package br.com.corely.finance.membershipplan;

import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.finance.membershipplan.dto.MembershipPlanRequest;
import br.com.corely.finance.membershipplan.dto.MembershipPlanResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipPlanServiceTest {

    @Mock
    private MembershipPlanRepository membershipPlanRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private ComercialTenantContext tenantContext;

    private MembershipPlanService service;

    private UUID studioId;
    private Studio studio;
    private MembershipPlan existingPlan;
    private UUID planId;

    @BeforeEach
    void setUp() {
        service = new MembershipPlanService(membershipPlanRepository, studioRepository, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);

        planId = UUID.randomUUID();
        existingPlan = new MembershipPlan();
        existingPlan.setId(planId);
        existingPlan.setStudio(studio);
        existingPlan.setName("Plano Básico");
        existingPlan.setDescription("Descrição");
        existingPlan.setMonthlyPrice(BigDecimal.valueOf(199));
        existingPlan.setSessionsPerWeek(2);
        existingPlan.setActive(true);
    }

    @Test
    void create_shouldPersistPlan() {
        var request = new MembershipPlanRequest();
        request.setName("Plano Premium");
        request.setDescription("Plano Premium");
        request.setMonthlyPrice(BigDecimal.valueOf(299));
        request.setSessionsPerWeek(3);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(membershipPlanRepository.existsByName(studioId, "Plano Premium")).thenReturn(false);
        when(membershipPlanRepository.save(any(MembershipPlan.class))).thenAnswer(inv -> {
            var p = inv.getArgument(0, MembershipPlan.class);
            p.setId(UUID.randomUUID());
            p.setActive(true);
            return p;
        });

        var response = service.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Plano Premium");
        assertThat(response.getMonthlyPrice()).isEqualByComparingTo(BigDecimal.valueOf(299));
        assertThat(response.getSessionsPerWeek()).isEqualTo(3);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void create_shouldThrowException_whenDuplicateName() {
        var request = new MembershipPlanRequest();
        request.setName("Plano Básico");
        request.setMonthlyPrice(BigDecimal.valueOf(199));
        request.setSessionsPerWeek(2);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(membershipPlanRepository.existsByName(studioId, "Plano Básico")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void findById_shouldReturnPlan() {
        when(membershipPlanRepository.findById(planId)).thenReturn(Optional.of(existingPlan));

        var response = service.findById(planId);

        assertThat(response.getId()).isEqualTo(planId);
        assertThat(response.getName()).isEqualTo("Plano Básico");
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        when(membershipPlanRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Membership plan not found");
    }

    @Test
    void findAll_shouldReturnAllPlans() {
        when(membershipPlanRepository.findAll()).thenReturn(List.of(existingPlan));

        var all = service.findAll();

        assertThat(all).hasSize(1);
    }

    @Test
    void findAllActive_shouldReturnOnlyActive() {
        when(membershipPlanRepository.findAllActive()).thenReturn(List.of(existingPlan));

        var all = service.findAllActive();

        assertThat(all).hasSize(1);
    }

    @Test
    void update_shouldModifyPlan() {
        var request = new MembershipPlanRequest();
        request.setName("Plano Atualizado");
        request.setDescription("Nova descrição");
        request.setMonthlyPrice(BigDecimal.valueOf(249));
        request.setSessionsPerWeek(3);

        when(membershipPlanRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(membershipPlanRepository.existsByNameAndIdNot(studioId, "Plano Atualizado", planId)).thenReturn(false);
        when(membershipPlanRepository.save(any(MembershipPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.update(planId, request);

        assertThat(response.getName()).isEqualTo("Plano Atualizado");
        assertThat(response.getMonthlyPrice()).isEqualByComparingTo(BigDecimal.valueOf(249));
        assertThat(response.getSessionsPerWeek()).isEqualTo(3);
    }

    @Test
    void update_shouldThrowException_whenDuplicateName() {
        var request = new MembershipPlanRequest();
        request.setName("Plano Duplicado");
        request.setMonthlyPrice(BigDecimal.valueOf(199));
        request.setSessionsPerWeek(2);

        when(membershipPlanRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(membershipPlanRepository.existsByNameAndIdNot(studioId, "Plano Duplicado", planId)).thenReturn(true);

        assertThatThrownBy(() -> service.update(planId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void delete_shouldPerformLogicalDeletion() {
        when(membershipPlanRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
        when(membershipPlanRepository.countStudentsByPlanId(planId)).thenReturn(0L);

        service.delete(planId);

        assertThat(existingPlan.getActive()).isFalse();
    }

    @Test
    void delete_shouldThrowException_whenPlanHasStudents() {
        when(membershipPlanRepository.findById(planId)).thenReturn(Optional.of(existingPlan));
        when(membershipPlanRepository.countStudentsByPlanId(planId)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(planId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("students linked");
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        when(membershipPlanRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Membership plan not found");
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        var request = new MembershipPlanRequest();
        request.setName("Teste");
        request.setMonthlyPrice(BigDecimal.valueOf(100));
        request.setSessionsPerWeek(1);

        when(membershipPlanRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Membership plan not found");
    }
}
