package br.com.corely.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de acesso ao Swagger/OpenAPI com o Swagger habilitado (EPIC-02-S04).
 *
 * <p>No profile {@code test} (default) o {@code corely.swagger.enabled} é
 * {@code true}, portanto a documentação ({@code /v3/api-docs}) e a UI
 * ({@code /swagger-ui/**}) devem ser acessíveis sem autenticação.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_withSwaggerEnabled_shouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUi_withSwaggerEnabled_shouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
