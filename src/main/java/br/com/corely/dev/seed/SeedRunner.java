package br.com.corely.dev.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

@RequiredArgsConstructor
@Slf4j
public class SeedRunner implements CommandLineRunner {

    private final SeedService seedService;

    @Override
    public void run(String... args) {
        log.info("=== SeedRunner: Iniciando carga de dados de desenvolvimento ===");
        try {
            SeedResponse response = seedService.execute();
            log.info("=== SeedRunner: Carga concluida ===");
            log.info("Estudantes: {}, Turmas: {}, Sessoes: {}, Presencas: {}, Reposicoes: {}",
                    response.getStudents(), response.getClassGroups(),
                    response.getSessions(), response.getAttendances(), response.getMakeupRequests());
        } catch (Exception e) {
            log.error("SeedRunner: Erro durante a carga de dados", e);
        }
    }
}
