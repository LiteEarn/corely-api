package br.com.corely.comercial.tenant;

import br.com.corely.auth.security.AuthenticationFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComercialTenantContextTest {

    @Mock
    private AuthenticationFacade authenticationFacade;

    @InjectMocks
    private ComercialTenantContext tenantContext;

    @Test
    void getCurrentStudioId_shouldReturnStudioIdFromAuthenticatedUser() {
        UUID expectedStudioId = UUID.randomUUID();
        when(authenticationFacade.getCurrentStudioId()).thenReturn(expectedStudioId);

        UUID result = tenantContext.getCurrentStudioId();

        assertThat(result).isEqualTo(expectedStudioId);
    }

    @Test
    void getCurrentStudioId_whenNotAuthenticated_shouldThrowTenantResolutionException() {
        when(authenticationFacade.getCurrentStudioId()).thenReturn(null);

        assertThatThrownBy(() -> tenantContext.getCurrentStudioId())
                .isInstanceOf(TenantResolutionException.class)
                .hasMessageContaining("Studio ID could not be resolved");
    }

    @Test
    void getCurrentStudioId_shouldNeverAcceptStudioIdFromParameter() {
        var methods = ComercialTenantContext.class.getDeclaredMethods();
        assertThat(methods)
                .filteredOn(m -> m.getParameterCount() > 0)
                .isEmpty();
    }
}
