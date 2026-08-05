package br.com.corely.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de configuração do CORS (EPIC-02-S08).
 */
@SpringBootTest
@ActiveProfiles("test")
class CorsConfigTest {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private CorsProperties corsProperties;

    @Test
    void corsConfiguration_shouldExposeAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/students");
        var config = corsConfigurationSource.getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getExposedHeaders()).contains("Authorization");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getAllowedMethods())
                .contains("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
    }

    @Test
    void corsConfiguration_shouldPropagateAllowedOriginsFromProperties() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/students");
        var config = corsConfigurationSource.getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins())
                .as("as origens permitidas devem vir de corely.cors.allowed-origins")
                .isEqualTo(corsProperties.getAllowedOrigins());
    }

    @Test
    void corsProperties_shouldLoadAllowedOriginsFromConfiguration() {
        assertThat(corsProperties.getAllowedOrigins())
                .as("application.yaml base define http://localhost:4200 no profile test")
                .contains("http://localhost:4200");
    }
}