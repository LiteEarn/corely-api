package br.com.corely.auth.security.ratelimit;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Testes unitários do {@link RateLimitFilter} (EPIC-02-S06).
 */
class RateLimitFilterTest {

    private RateLimiter rateLimiter;
    private RateLimitProperties properties;
    private RateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
        properties = new RateLimitProperties();
        filter = new RateLimitFilter(rateLimiter, properties);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void doFilter_shouldPassThroughWhenDisabled() throws Exception {
        properties.setEnabled(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_shouldAllowRequestsWithinSensitiveLimit() throws Exception {
        properties.setSensitiveRequestsPerWindow(3);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("10.0.0.1");

        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);
            assertThat(response.getStatus()).as("requisição %d", i + 1).isEqualTo(200);
        }
    }

    @Test
    void doFilter_shouldReturn429WhenSensitiveLimitExceeded() throws Exception {
        properties.setSensitiveRequestsPerWindow(2);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("10.0.0.1");

        for (int i = 0; i < 2; i++) {
            filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilter_shouldUseGlobalLimitForNonSensitivePath() throws Exception {
        properties.setRequestsPerWindow(2);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/students");
        request.setRemoteAddr("10.0.0.1");

        for (int i = 0; i < 2; i++) {
            filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void doFilter_exhaustingSensitiveLimit_shouldNotBlockGlobalScope() throws Exception {
        properties.setSensitiveRequestsPerWindow(1);
        properties.setRequestsPerWindow(100);
        MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/auth/login");
        loginRequest.setRemoteAddr("10.0.0.1");

        // Esgota o limite sensível.
        filter.doFilter(loginRequest, new MockHttpServletResponse(), filterChain);
        MockHttpServletResponse blockedLogin = new MockHttpServletResponse();
        filter.doFilter(loginRequest, blockedLogin, filterChain);
        assertThat(blockedLogin.getStatus()).isEqualTo(429);

        // Endpoints globais permanecem acessíveis para o mesmo IP.
        MockHttpServletRequest globalRequest = new MockHttpServletRequest("GET", "/api/students");
        globalRequest.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse globalResponse = new MockHttpServletResponse();
        filter.doFilter(globalRequest, globalResponse, filterChain);
        assertThat(globalResponse.getStatus())
                .as("esgotar o limite sensível não deve bloquear o escopo global")
                .isEqualTo(200);
    }

    @Test
    void doFilter_globalTraffic_shouldNotRefillSensitiveScope() throws Exception {
        properties.setSensitiveRequestsPerWindow(1);
        properties.setRequestsPerWindow(100);
        MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/auth/login");
        loginRequest.setRemoteAddr("10.0.0.1");

        // Esgota o limite sensível.
        filter.doFilter(loginRequest, new MockHttpServletResponse(), filterChain);
        assertThat(newRateLimitResponse(loginRequest).getStatus()).isEqualTo(429);

        // Tráfego global intercalado não deve reabastecer o limite sensível.
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest globalRequest = new MockHttpServletRequest("GET", "/api/students");
            globalRequest.setRemoteAddr("10.0.0.1");
            filter.doFilter(globalRequest, new MockHttpServletResponse(), filterChain);
        }

        assertThat(newRateLimitResponse(loginRequest).getStatus())
                .as("tráfego global não deve reabastecer o limite sensível")
                .isEqualTo(429);
    }

    @Test
    void doFilter_shouldResolveClientIpFromForwardedHeaderWhenTrusted() throws Exception {
        properties.setSensitiveRequestsPerWindow(1);
        properties.setTrustForwardedHeader(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus())
                .as("IP do X-Forwarded-For deve ser usado como chave quando confiável")
                .isEqualTo(429);
    }

    @Test
    void doFilter_shouldIgnoreForwardedHeaderWhenNotTrusted() throws Exception {
        properties.setSensitiveRequestsPerWindow(1);
        properties.setTrustForwardedHeader(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.5");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus())
                .as("X-Forwarded-For não deve ser usado quando trust-forwarded-header=false")
                .isEqualTo(429);
    }

    @Test
    void doFilter_shouldNotConsumeTokensForOptionsPreflight() throws Exception {
        properties.setSensitiveRequestsPerWindow(1);
        MockHttpServletRequest preflight = new MockHttpServletRequest("OPTIONS", "/auth/login");
        preflight.setRemoteAddr("10.0.0.1");

        // Preflight não consome token.
        MockHttpServletResponse preflightResponse = new MockHttpServletResponse();
        filter.doFilter(preflight, preflightResponse, filterChain);
        assertThat(preflightResponse.getStatus()).isEqualTo(200);

        // Primeiro POST passa, segundo é bloqueado (1 token restante foi preservado).
        MockHttpServletRequest login = new MockHttpServletRequest("POST", "/auth/login");
        login.setRemoteAddr("10.0.0.1");
        filter.doFilter(login, new MockHttpServletResponse(), filterChain);

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(login, blocked, filterChain);
        assertThat(blocked.getStatus())
                .as("preflight OPTIONS não deve consumir tokens do limite sensível")
                .isEqualTo(429);
    }

    private MockHttpServletResponse newRateLimitResponse(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);
        return response;
    }
}