package br.com.corely.comercial.delinquencypolicy;

import br.com.corely.comercial.delinquencypolicy.dto.DelinquencyActionDto;
import br.com.corely.comercial.delinquencypolicy.dto.DelinquencyPolicyRequest;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelinquencyPolicyServiceTest {

    @Mock
    private DelinquencyPolicyRepository delinquencyPolicyRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private ComercialTenantContext tenantContext;

    private DelinquencyPolicyService service;

    private UUID studioId;
    private Studio studio;

    @BeforeEach
    void setUp() {
        service = new DelinquencyPolicyService(delinquencyPolicyRepository, studioRepository, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);
    }

    @Test
    void getOrCreate_shouldReturnExistingPolicy() {
        var policy = new DelinquencyPolicy();
        policy.setId(UUID.randomUUID());
        policy.setStudio(studio);
        policy.setGracePeriodDays(5);
        policy.setAction(DelinquencyAction.NONE);
        policy.setActive(true);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(delinquencyPolicyRepository.findByStudioId(studioId)).thenReturn(Optional.of(policy));

        var response = service.getOrCreate();

        assertThat(response.getGracePeriodDays()).isEqualTo(5);
        assertThat(response.getAction()).isEqualTo(DelinquencyActionDto.NONE);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void getOrCreate_shouldCreatePolicyWhenNotExists() {
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(delinquencyPolicyRepository.findByStudioId(studioId)).thenReturn(Optional.empty());
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(delinquencyPolicyRepository.save(any(DelinquencyPolicy.class))).thenAnswer(inv -> {
            var p = inv.getArgument(0, DelinquencyPolicy.class);
            p.setId(UUID.randomUUID());
            return p;
        });

        var response = service.getOrCreate();

        assertThat(response.getGracePeriodDays()).isEqualTo(0);
        assertThat(response.getAction()).isEqualTo(DelinquencyActionDto.NONE);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void update_shouldChangeGracePeriodAndAction() {
        var policy = new DelinquencyPolicy();
        policy.setId(UUID.randomUUID());
        policy.setStudio(studio);
        policy.setGracePeriodDays(0);
        policy.setAction(DelinquencyAction.NONE);
        policy.setActive(true);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(delinquencyPolicyRepository.findByStudioId(studioId)).thenReturn(Optional.of(policy));
        when(delinquencyPolicyRepository.save(any(DelinquencyPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new DelinquencyPolicyRequest();
        request.setGracePeriodDays(7);
        request.setAction(DelinquencyActionDto.SUSPEND_CONTRACT);
        request.setActive(true);

        var response = service.update(request);

        assertThat(response.getGracePeriodDays()).isEqualTo(7);
        assertThat(response.getAction()).isEqualTo(DelinquencyActionDto.SUSPEND_CONTRACT);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(delinquencyPolicyRepository.findByStudioId(studioId)).thenReturn(Optional.empty());

        var request = new DelinquencyPolicyRequest();
        request.setGracePeriodDays(5);
        request.setAction(DelinquencyActionDto.NONE);

        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Delinquency policy not found");
    }
}
