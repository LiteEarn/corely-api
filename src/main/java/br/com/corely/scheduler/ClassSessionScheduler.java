package br.com.corely.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClassSessionScheduler {

    private final SessionGenerationService sessionGenerationService;

    @Scheduled(cron = "0 0 3 * * *")
    public void generateSessions() {
        log.info("ClassSessionScheduler: starting daily session generation at 03:00");
        var response = sessionGenerationService.generateForAllActiveGroups();
        log.info("ClassSessionScheduler: completed - created={}, ignored={}",
                response.getCreated(), response.getIgnored());
    }
}
