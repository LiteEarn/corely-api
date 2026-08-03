package br.com.corely.auth.security.jwt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de segurança da configuração do JWT (EPIC-02-S01).
 *
 * Garantem que o JWT secret nunca está embutido em configuração versionada:
 * o segredo de produção deve ser resolvido exclusivamente da variável de
 * ambiente {@code JWT_SECRET}.
 */
class JwtSecretConfigTest {

    private static final String LEGACY_SECRET = "my-secret-key-for-corely-application-2024";

    @Test
    void applicationYaml_shouldResolveSecretFromEnvironmentVariable() throws IOException {
        String applicationYaml = readClasspathResource("/application.yaml");

        assertThat(applicationYaml)
                .as("jwt.secret deve usar a variável de ambiente JWT_SECRET")
                .contains("secret: ${JWT_SECRET}");
    }

    @Test
    void applicationYaml_shouldNotContainHardcodedSecret() throws IOException {
        String applicationYaml = readClasspathResource("/application.yaml");

        assertThat(applicationYaml)
                .as("application.yaml não deve conter segredo embutido")
                .doesNotContain(LEGACY_SECRET);
    }

    @Test
    void devProfile_shouldOnlyAllowDevOnlyDefault() throws IOException {
        String applicationDevYaml = readClasspathResource("/application-dev.yaml");

        assertThat(applicationDevYaml)
                .as("application-dev.yaml só pode ter default claramente dev-only")
                .contains("${JWT_SECRET:dev-only-secret-not-for-production-corely}")
                .doesNotContain(LEGACY_SECRET);
    }

    @Test
    void testProfile_shouldUseTestOnlySecret() throws IOException {
        String applicationTestProperties = readClasspathResource("/application-test.properties");

        assertThat(applicationTestProperties)
                .as("application-test.properties deve usar segredo exclusivo de teste")
                .contains("test-only-secret-key-for-corely-jwt-2024")
                .doesNotContain(LEGACY_SECRET);
    }

    @Test
    void applicationYaml_shouldSupportSecretRotationViaEnvironmentVariable() throws IOException {
        String applicationYaml = readClasspathResource("/application.yaml");

        assertThat(applicationYaml)
                .as("jwt.previous-secrets deve ser resolvido de JWT_PREVIOUS_SECRETS")
                .contains("previous-secrets: ${JWT_PREVIOUS_SECRETS:}");
    }

    @Test
    void devProfile_shouldSupportSecretRotationViaEnvironmentVariable() throws IOException {
        String applicationDevYaml = readClasspathResource("/application-dev.yaml");

        assertThat(applicationDevYaml)
                .as("application-dev.yaml deve expor jwt.previous-secrets sem default versionado")
                .contains("previous-secrets: ${JWT_PREVIOUS_SECRETS:}");
    }

    private String readClasspathResource(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertThat(in).as("recurso %s deve existir no classpath", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
