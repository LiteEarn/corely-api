package br.com.corely.scheduler;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.classsession.dto.SessionGenerationResponse;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionGenerationServiceTest {

    @Autowired
    private SessionGenerationService sessionGenerationService;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    private Studio studio;
    private Instructor instructor;
    private ClassGroup classGroup;

    @BeforeEach
    void setUp() {
        classSessionRepository.deleteAll();
        classGroupRepository.deleteAll();
        instructorRepository.deleteAll();
        studioRepository.deleteAll();

        studio = new Studio();
        studio.setName("Test Studio");
        studio = studioRepository.save(studio);

        instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName("Test Instructor");
        instructor.setEmail("instructor@test.com");
        instructor.setActive(true);
        instructor = instructorRepository.save(instructor);
    }

    private ClassGroup createClassGroup(LocalDate startDate, LocalDate endDate, boolean... days) {
        ClassGroup cg = new ClassGroup();
        cg.setStudio(studio);
        cg.setInstructor(instructor);
        cg.setName("Test Class Group");
        cg.setStartTime(LocalTime.of(10, 0));
        cg.setEndTime(LocalTime.of(11, 0));
        cg.setCapacity(10);
        cg.setActive(true);
        cg.setStartDate(startDate);
        cg.setEndDate(endDate);
        if (days.length > 0) {
            cg.setMonday(days.length > 0 && days[0]);
            cg.setTuesday(days.length > 1 && days[1]);
            cg.setWednesday(days.length > 2 && days[2]);
            cg.setThursday(days.length > 3 && days[3]);
            cg.setFriday(days.length > 4 && days[4]);
            cg.setSaturday(days.length > 5 && days[5]);
            cg.setSunday(days.length > 6 && days[6]);
        } else {
            cg.setMonday(true);
        }
        return classGroupRepository.save(cg);
    }

    @Test
    void generateForGroup_simpleGeneration_createsSessions() {
        classGroup = createClassGroup(LocalDate.now(), LocalDate.now().plusDays(30), true, false, true, false, true, false, false);
        SessionGenerationResponse response = sessionGenerationService.generateForGroup(classGroup);
        long expectedMondays = LocalDate.now().datesUntil(LocalDate.now().plusDays(31))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.MONDAY).count();
        long expectedWednesdays = LocalDate.now().datesUntil(LocalDate.now().plusDays(31))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.WEDNESDAY).count();
        long expectedFridays = LocalDate.now().datesUntil(LocalDate.now().plusDays(31))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.FRIDAY).count();
        assertThat(response.getCreated()).isEqualTo(expectedMondays + expectedWednesdays + expectedFridays);
        assertThat(response.getIgnored()).isZero();
    }

    @Test
    void generateForGroup_multipleDaysOfWeek_createsSessionsForAll() {
        classGroup = createClassGroup(LocalDate.now(), LocalDate.now().plusDays(14), true, true, true, true, true, false, false);
        SessionGenerationResponse response = sessionGenerationService.generateForGroup(classGroup);
        long expected = LocalDate.now().datesUntil(LocalDate.now().plusDays(15))
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();
        assertThat(response.getCreated()).isEqualTo((int) expected);
    }

    @Test
    void generateForGroup_respectsDateRange() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(10);
        classGroup = createClassGroup(start, end, true, false, false, false, false, false, false);
        SessionGenerationResponse response = sessionGenerationService.generateForGroup(classGroup);
        long expectedMondays = start.datesUntil(end.plusDays(1))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.MONDAY).count();
        assertThat(response.getCreated()).isEqualTo((int) expectedMondays);
    }

    @Test
    void generateForGroup_preventsDuplicates() {
        classGroup = createClassGroup(LocalDate.now(), LocalDate.now().plusDays(30), true, false, false, false, false, false, false);
        sessionGenerationService.generateForGroup(classGroup);
        SessionGenerationResponse response = sessionGenerationService.generateForGroup(classGroup);
        assertThat(response.getCreated()).isZero();
        assertThat(response.getIgnored()).isPositive();
    }

    @Test
    void generateForGroup_doesNotGeneratePastDates() {
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate end = LocalDate.now().plusDays(10);
        classGroup = createClassGroup(start, end, true, true, true, true, true, true, true);
        SessionGenerationResponse response = sessionGenerationService.generateForGroup(classGroup);
        long expected = LocalDate.now().datesUntil(end.plusDays(1)).count();
        assertThat(response.getCreated()).isEqualTo((int) expected);
    }

    @Test
    void generateForGroup_whenEndDateBeforeToday_returnsZero() {
        classGroup = createClassGroup(LocalDate.now().minusDays(20), LocalDate.now().minusDays(10), true, false, false, false, false, false, false);
        SessionGenerationResponse response = sessionGenerationService.generateForGroup(classGroup);
        assertThat(response.getCreated()).isZero();
        assertThat(response.getIgnored()).isZero();
    }

    @Test
    void generateForGroup_withoutDateRange_usesConfigurableLookahead() {
        classGroup = createClassGroup(null, null, true, true, true, true, true, true, true);
        SessionGenerationResponse response = sessionGenerationService.generateForGroup(classGroup);
        assertThat(response.getCreated()).isEqualTo(91);
    }

    @Test
    void generateForAllActiveGroups_generatesForAllActive() {
        ClassGroup g1 = createClassGroup(LocalDate.now(), LocalDate.now().plusDays(5), true, false, false, false, false, false, false);
        ClassGroup g2 = createClassGroup(LocalDate.now(), LocalDate.now().plusDays(5), false, true, false, false, false, false, false);
        SessionGenerationResponse response = sessionGenerationService.generateForAllActiveGroups();
        long expectedG1 = LocalDate.now().datesUntil(LocalDate.now().plusDays(6))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.MONDAY).count();
        long expectedG2 = LocalDate.now().datesUntil(LocalDate.now().plusDays(6))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.TUESDAY).count();
        assertThat(response.getCreated()).isEqualTo((int) (expectedG1 + expectedG2));
    }

    @Test
    void generateForAllActiveGroups_skipsInactiveGroups() {
        classGroup = createClassGroup(LocalDate.now(), LocalDate.now().plusDays(5), true, false, false, false, false, false, false);
        classGroup.setActive(false);
        classGroupRepository.save(classGroup);
        SessionGenerationResponse response = sessionGenerationService.generateForAllActiveGroups();
        assertThat(response.getCreated()).isZero();
    }

    @Test
    void regenerateForClassGroup_removesFutureScheduledAndRegenerates() {
        classGroup = createClassGroup(LocalDate.now(), LocalDate.now().plusDays(30), true, false, false, false, false, false, false);
        sessionGenerationService.generateForGroup(classGroup);
        classGroup.setTuesday(true);
        classGroupRepository.save(classGroup);
        SessionGenerationResponse response = sessionGenerationService.regenerateForClassGroup(classGroup.getId());
        assertThat(response.getCreated()).isPositive();
        long totalForMonTue = LocalDate.now().datesUntil(LocalDate.now().plusDays(31))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.MONDAY || d.getDayOfWeek() == DayOfWeek.TUESDAY)
                .count();
        assertThat(classSessionRepository.count()).isEqualTo(totalForMonTue);
    }

    @Test
    void generateForGroup_ignoresNonActiveDays() {
        classGroup = createClassGroup(LocalDate.now(), LocalDate.now().plusDays(10), false, false, false, false, false, false, false);
        SessionGenerationResponse response = sessionGenerationService.generateForGroup(classGroup);
        assertThat(response.getCreated()).isZero();
    }

    @Test
    void generateForGroup_handlesPartialDateRange() {
        classGroup = createClassGroup(LocalDate.now().plusDays(3), LocalDate.now().plusDays(7), true, true, true, true, true, true, true);
        SessionGenerationResponse response = sessionGenerationService.generateForGroup(classGroup);
        assertThat(response.getCreated()).isEqualTo(5);
    }
}
