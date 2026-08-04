package br.com.corely.dev.seed;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuração do seed de dados de desenvolvimento.
 *
 * <p>O seed automático roda <b>apenas no perfil {@code dev}</b> e apenas quando
 * habilitado ({@code corely.seed.enabled}). O perfil é o gate obrigatório: mesmo
 * que {@code corely.seed.enabled} seja definido como {@code true} em outro
 * ambiente (ex.: produção), o seed <b>nunca</b> executa fora do perfil dev
 * (EPIC-02-S05).</p>
 */
@Configuration
public class SeedConfiguration {

    @Bean
    @Profile("dev")
    @ConditionalOnProperty(prefix = "corely.seed", name = "enabled", havingValue = "true")
    public SeedRunner seedRunner(SeedService seedService) {
        return new SeedRunner(seedService);
    }
}
