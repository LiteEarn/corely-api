package br.com.corely.dev.seed;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Testes do seed restrito ao perfil dev (EPIC-02-S05).
 *
 * <p>Garante a regra fail-closed: o {@code SeedRunner} e o {@code SeedController}
 * existem apenas quando o perfil ativo é {@code dev} (e, para o seed automático,
 * também com {@code corely.seed.enabled=true}). Em qualquer outro ambiente —
 * incluindo produção com {@code corely.seed.enabled} definido como {@code true} —
 * o seed <b>nunca</b> é criado nem exposto via {@code /dev/seed/**}.</p>
 */
class SeedConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SeedConfiguration.class, SeedController.class)
            .withBean(SeedService.class, () -> mock(SeedService.class));

    @Test
    void seedComponents_shouldBeCreated_whenDevProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev", "corely.seed.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(SeedRunner.class)
                        .hasSingleBean(SeedController.class));
    }

    @Test
    void seedComponents_shouldBeCreated_whenDevAmongMultipleProfiles() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev,local", "corely.seed.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(SeedRunner.class)
                        .hasSingleBean(SeedController.class));
    }

    @Test
    void seedRunner_shouldNotBeCreated_whenDevProfileButDisabled() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev", "corely.seed.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(SeedRunner.class)
                        .hasSingleBean(SeedController.class));
    }

    @Test
    void seedComponents_shouldNotBeCreated_whenProdProfileEvenIfEnabled() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod", "corely.seed.enabled=true")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(SeedRunner.class)
                        .doesNotHaveBean(SeedController.class));
    }

    @Test
    void seedComponents_shouldNotBeCreated_whenNoProfileEvenIfEnabled() {
        contextRunner
                .withPropertyValues("corely.seed.enabled=true")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(SeedRunner.class)
                        .doesNotHaveBean(SeedController.class));
    }
}
