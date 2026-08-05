package br.com.corely.auth.security.jwt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de segurança da configuração do profile de produção (EPIC-02-S03/S04).
 *
 * Garantem que o profile {@code prod} é isolado e seguro: datasource via
 * variáveis de ambiente (sem credenciais embutidas), SQL logging desabilitado,
 * seed de dados desabilitado, Swagger/OpenAPI desabilitado, schema validado
 * (nunca alterado) e stacktraces não expostos em respostas de erro.
 */
class ProdProfileConfigTest {

    private static final String LOCAL_CREDENTIALS = "password: corely";

    @Test
    void prodProfile_shouldResolveDatasourceFromEnvironmentVariables() throws IOException {
        String prodYaml = readClasspathResource("/application-prod.yaml");

        assertThat(prodYaml)
                .as("datasource.url deve vir de DATABASE_URL (sem default)")
                .contains("url: ${DATABASE_URL}")
                .contains("username: ${DATABASE_USERNAME}")
                .contains("password: ${DATABASE_PASSWORD}");
    }

    @Test
    void prodProfile_shouldNotContainHardcodedCredentials() throws IOException {
        String prodYaml = readClasspathResource("/application-prod.yaml");

        assertThat(prodYaml)
                .as("application-prod.yaml não deve conter credenciais embutidas")
                .doesNotContain(LOCAL_CREDENTIALS)
                .doesNotContain("localhost:5432");
    }

    @Test
    void prodProfile_shouldDisableSqlLogging() throws IOException {
        String prodYaml = readClasspathResource("/application-prod.yaml");

        assertThat(prodYaml)
                .as("profile prod deve desabilitar show-sql e format_sql")
                .contains("show-sql: false")
                .contains("format_sql: false");
    }

    @Test
    void prodProfile_shouldValidateSchemaWithoutAlteringIt() throws IOException {
        String prodYaml = readClasspathResource("/application-prod.yaml");

        assertThat(prodYaml)
                .as("profile prod deve usar ddl-auto: validate")
                .contains("ddl-auto: validate")
                .doesNotContain("ddl-auto: create")
                .doesNotContain("ddl-auto: update");
    }

    @Test
    void prodProfile_shouldDisableSeed() throws IOException {
        String prodYaml = readClasspathResource("/application-prod.yaml");

        assertThat(prodYaml)
                .as("corely.seed.enabled deve estar desabilitado no profile prod")
                .containsPattern("(?s)corely:\\s*\\n\\s*seed:\\s*\\n\\s*enabled: false");
    }

    @Test
    void prodProfile_shouldDisableSpringdocApiDocs() throws IOException {
        String prodYaml = readClasspathResource("/application-prod.yaml");

        assertThat(prodYaml)
                .as("springdoc api-docs deve estar desabilitado no profile prod")
                .containsPattern("(?s)springdoc:\\s*\\n\\s*api-docs:\\s*\\n\\s*enabled: false");
    }

    @Test
    void prodProfile_shouldDisableSpringdocSwaggerUi() throws IOException {
        String prodYaml = readClasspathResource("/application-prod.yaml");

        assertThat(prodYaml)
                .as("springdoc swagger-ui deve estar desabilitado no profile prod")
                .containsPattern("(?s)springdoc:\\s*\\n\\s*api-docs:\\s*\\n\\s*enabled: false[\\s\\S]*?swagger-ui:\\s*\\n\\s*enabled: false");
    }

    @Test
    void prodProfile_shouldRequireAuthForSwaggerPaths() throws IOException {
        String prodYaml = readClasspathResource("/application-prod.yaml");

        assertThat(prodYaml)
                .as("corely.swagger.enabled deve estar desabilitado no profile prod")
                .containsPattern("(?m)^\\s*swagger:\\s*\\n\\s*enabled: false$");
    }

    @Test
    void prodProfile_shouldEnableRateLimit() throws IOException {
        String prodYaml = readClasspathResource("/application-prod.yaml");

        assertThat(prodYaml)
                .as("corely.rate-limit.enabled deve estar habilitado no profile prod")
                .containsPattern("(?s)rate-limit:\\s*\\n\\s*enabled: true");
    }

    @Test
    void prodProfile_shouldNotExposeStacktraces() throws IOException {
        String prodYaml = readClasspathResource("/application-prod.yaml");

        assertThat(prodYaml)
                .as("profile prod não deve expor stacktraces em erros")
                .contains("include-stacktrace: never");
    }

    private String readClasspathResource(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertThat(in).as("recurso %s deve existir no classpath", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
