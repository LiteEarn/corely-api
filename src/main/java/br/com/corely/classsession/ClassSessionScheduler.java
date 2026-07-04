package br.com.corely.classsession;

import br.com.corely.classgroup.ClassGroupRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClassSessionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClassSessionScheduler.class);

    private final ClassGroupRepository classGroupRepository;
    private final ClassSessionService classSessionService;

    @Scheduled(cron = "0 0 2 * * *")
    public void generateSessions() {
        var activeGroups = classGroupRepository.findByActiveTrue();
        int totalCreated = 0;

        for (var group : activeGroups) {
            try {
                var response = classSessionService.generateSessionsForGroup(group);
                totalCreated += response.getCreated();
            } catch (Exception e) {
                log.error("ClassSessionScheduler: error generating sessions for class group {}", group.getId(), e);
            }
        }

        log.info("ClassSessionScheduler: created {} sessions across {} active class groups",
                totalCreated, activeGroups.size());
    }
}
