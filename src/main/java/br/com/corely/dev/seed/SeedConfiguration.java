package br.com.corely.dev.seed;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedConfiguration {

    @Bean
    @ConditionalOnExpression("'${spring.profiles.active:}' == 'dev' || '${corely.seed.enabled:false}' == 'true'")
    public SeedRunner seedRunner(SeedService seedService) {
        return new SeedRunner(seedService);
    }
}
