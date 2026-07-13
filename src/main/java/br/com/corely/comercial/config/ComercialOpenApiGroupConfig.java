package br.com.corely.comercial.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ComercialOpenApiGroupConfig {

    @Bean
    public GroupedOpenApi comercialGroupOpenApi() {
        return GroupedOpenApi.builder()
                .group("comercial")
                .displayName("Módulo Comercial")
                .pathsToMatch("/comercial/**")
                .build();
    }
}
