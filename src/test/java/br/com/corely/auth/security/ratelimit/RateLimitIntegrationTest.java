package br.com.corely.auth.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração do rate limiting pela chain HTTP completa (EPIC-02-S06).
 *
 * <p>Habilita o filtro via {@code @TestPropertySource} (o profile {@code test}
 * o desabilita por padrão) e valida que o {@code 429 Too Many Requests} com o
 * header {@code Retry-After} é produzido quando o limite sensível de
 * {@code /auth/**} é esgotado.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "corely.rate-limit.enabled=true",
        "corely.rate-limit.sensitive-requests-per-window=2",
        "corely.rate-limit.window-seconds=60"
})
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_shouldReturn429WithRetryAfter_whenSensitiveLimitExceeded() throws Exception {
        String loginBody = """
                {"email":"nao-existe@corely.com","password":"senha-errada"}
                """;

        // Primeira requisição: permitida (passa pela chain, retorna 401 de credenciais inválidas).
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());

        // Segunda requisição: permitida.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());

        // Terceira requisição: limite sensível (2) esgotado → 429 + Retry-After.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }
}