package br.com.corely.comercial.tenant;

import br.com.corely.shared.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComercialTenantContextTest {

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private ComercialTenantContext comercialTenantContext;

    @Test
    void getCurrentStudioId_shouldDelegateToGlobalTenantContext() {
        UUID expectedStudioId = UUID.randomUUID();
        when(tenantContext.getCurrentStudioId()).thenReturn(expectedStudioId);

        UUID result = comercialTenantContext.getCurrentStudioId();

        assertThat(result).isEqualTo(expectedStudioId);
    }

    @Test
    void getCurrentUserId_shouldDelegateToGlobalTenantContext() {
        UUID expectedUserId = UUID.randomUUID();
        when(tenantContext.getCurrentUserId()).thenReturn(expectedUserId);

        UUID result = comercialTenantContext.getCurrentUserId();

        assertThat(result).isEqualTo(expectedUserId);
    }

    @Test
    void getCurrentStudioId_shouldNeverAcceptStudioIdFromParameter() {
        var methods = ComercialTenantContext.class.getDeclaredMethods();
        assertThat(methods)
                .filteredOn(m -> m.getParameterCount() > 0)
                .isEmpty();
    }
}
