package br.com.corely.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de acesso ao Swagger/OpenAPI com o Swagger desabilitado (EPIC-02-S04).
 *
 * <p>Simula o cenário de produção ({@code corely.swagger.enabled: false}): a
 * documentação ({@code /v3/api-docs}) e a UI ({@code /swagger-ui/**}) deixam de
 * ser públicas e passam a exigir autenticação — requisições sem token são
 * rejeitadas.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "corely.swagger.enabled=false")
class SwaggerSecurityDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_withSwaggerDisabled_withoutToken_shouldBeRejected() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void swaggerUi_withSwaggerDisabled_withoutToken_shouldBeRejected() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isForbidden());
    }
}
