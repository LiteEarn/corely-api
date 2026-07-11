package br.com.corely.scheduler;

import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.classsession.dto.SessionGenerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionGenerationService {

    private final ClassGroupRepository classGroupRepository;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;

    @Value("${corely.scheduler.session-generation-days:90}")
    private int sessionGenerationDays;

    private static final int BATCH_SIZE = 50;

    private static final Map<DayOfWeek, Function<ClassGroup, Boolean>> DAY_OF_WEEK_MAP = Map.of(
            DayOfWeek.MONDAY, ClassGroup::getMonday,
            DayOfWeek.TUESDAY, ClassGroup::getTuesday,
            DayOfWeek.WEDNESDAY, ClassGroup::getWednesday,
            DayOfWeek.THURSDAY, ClassGroup::getThursday,
            DayOfWeek.FRIDAY, ClassGroup::getFriday,
            DayOfWeek.SATURDAY, ClassGroup::getSaturday,
            DayOfWeek.SUNDAY, ClassGroup::getSunday
    );

    @Transactional
    public SessionGenerationResponse generateForClassGroup(UUID classGroupId) {
        ClassGroup classGroup = classGroupRepository.findById(classGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Class group not found: " + classGroupId));
        return generateForGroup(classGroup);
    }

    @Transactional
    public SessionGenerationResponse generateForGroup(ClassGroup classGroup) {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("SessionGenerationService: generating sessions for class group {} ({})",
                classGroup.getId(), classGroup.getName());

        LocalDate today = LocalDate.now();
        LocalDate rangeStart = classGroup.getStartDate() != null
                ? today.isAfter(classGroup.getStartDate()) ? today : classGroup.getStartDate()
                : today;
        LocalDate rangeEnd = classGroup.getEndDate() != null
                ? today.plusDays(sessionGenerationDays).isBefore(classGroup.getEndDate())
                    ? today.plusDays(sessionGenerationDays)
                    : classGroup.getEndDate()
                : today.plusDays(sessionGenerationDays);

        if (rangeStart.isAfter(rangeEnd)) {
            log.info("SessionGenerationService: no sessions to generate for group {} (range start {} after end {})",
                    classGroup.getId(), rangeStart, rangeEnd);
            return new SessionGenerationResponse(0, 0);
        }

        Set<LocalDate> existingDates = classSessionRepository
                .findByClassGroupIdAndSessionDateBetween(classGroup.getId(), rangeStart, rangeEnd)
                .stream()
                .map(ClassSession::getSessionDate)
                .collect(Collectors.toSet());

        int created = 0;
        int ignored = 0;
        List<ClassSession> batch = new ArrayList<>();

        for (LocalDate date = rangeStart; !date.isAfter(rangeEnd); date = date.plusDays(1)) {
            Boolean active = DAY_OF_WEEK_MAP.get(date.getDayOfWeek()).apply(classGroup);
            if (!Boolean.TRUE.equals(active)) {
                continue;
            }
            if (existingDates.contains(date)) {
                ignored++;
                continue;
            }

            ClassSession session = new ClassSession();
            session.setClassGroup(classGroup);
            session.setInstructor(classGroup.getInstructor());
            session.setSessionDate(date);
            session.setStartTime(classGroup.getStartTime());
            session.setEndTime(classGroup.getEndTime());
            session.setStatus(ClassSessionStatus.SCHEDULED);
            batch.add(session);
            created++;

            if (batch.size() >= BATCH_SIZE) {
                classSessionRepository.saveAll(batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            classSessionRepository.saveAll(batch);
        }

        long elapsed = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
        log.info("SessionGenerationService: completed for group {} - created={}, ignored={}, time={}ms",
                classGroup.getId(), created, ignored, elapsed);

        return new SessionGenerationResponse(created, ignored);
    }

    @Transactional
    public SessionGenerationResponse generateForAllActiveGroups() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("SessionGenerationService: generating sessions for all active class groups");

        List<ClassGroup> activeGroups = classGroupRepository.findByActiveTrue();
        int totalCreated = 0;
        int totalIgnored = 0;

        for (ClassGroup group : activeGroups) {
            try {
                SessionGenerationResponse response = generateForGroup(group);
                totalCreated += response.getCreated();
                totalIgnored += response.getIgnored();
            } catch (Exception e) {
                log.error("SessionGenerationService: error generating sessions for group {}: {}",
                        group.getId(), e.getMessage(), e);
            }
        }

        long elapsed = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
        log.info("SessionGenerationService: completed for all groups - created={}, ignored={}, groups={}, time={}ms",
                totalCreated, totalIgnored, activeGroups.size(), elapsed);

        return new SessionGenerationResponse(totalCreated, totalIgnored);
    }

    @Transactional
    public SessionGenerationResponse regenerateForClassGroup(UUID classGroupId) {
        ClassGroup classGroup = classGroupRepository.findById(classGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Class group not found: " + classGroupId));

        LocalDateTime startTime = LocalDateTime.now();
        log.info("SessionGenerationService: regenerating sessions for class group {} ({})",
                classGroup.getId(), classGroup.getName());

        List<ClassSession> futureScheduled = classSessionRepository
                .findByClassGroupIdAndSessionDateGreaterThanEqualAndStatus(
                        classGroupId, LocalDate.now(), ClassSessionStatus.SCHEDULED);

        int removed = 0;
        int keptWithAttendance = 0;

        for (ClassSession session : futureScheduled) {
            boolean hasAttendance = attendanceRepository.findByClassSessionId(session.getId()).isEmpty() == false;
            if (hasAttendance) {
                keptWithAttendance++;
                continue;
            }
            classSessionRepository.delete(session);
            removed++;
        }

        List<ClassSession> futureCancelled = classSessionRepository
                .findByClassGroupIdAndSessionDateGreaterThanEqualAndStatus(
                        classGroupId, LocalDate.now(), ClassSessionStatus.CANCELLED);
        if (!futureCancelled.isEmpty()) {
            classSessionRepository.deleteAll(futureCancelled);
        }

        log.info("SessionGenerationService: regeneration removed {} future scheduled sessions, kept {} with attendance",
                removed, keptWithAttendance);

        SessionGenerationResponse generated = generateForGroup(classGroup);

        long elapsed = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
        log.info("SessionGenerationService: regeneration completed for group {} - created={}, ignored={}, removed={}, time={}ms",
                classGroup.getId(), generated.getCreated(), generated.getIgnored(), removed, elapsed);

        return generated;
    }
}
