package br.com.corely.classsession;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
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
class ClassSessionSchedulerTest {

    @Autowired
    private ClassSessionScheduler classSessionScheduler;

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

        classGroup = new ClassGroup();
        classGroup.setStudio(studio);
        classGroup.setInstructor(instructor);
        classGroup.setName("Test Class Group");
        classGroup.setStartTime(LocalTime.of(10, 0));
        classGroup.setEndTime(LocalTime.of(11, 0));
        classGroup.setCapacity(10);
        classGroup.setMonday(true);
        classGroup.setActive(true);
        classGroup = classGroupRepository.save(classGroup);
    }

    @Test
    void generateSessions_createsSessionsForAllActiveDays() {
        classSessionScheduler.generateSessions();

        long expectedMondays = LocalDate.now().datesUntil(LocalDate.now().plusDays(61))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.MONDAY)
                .count();

        assertThat(classSessionRepository.count()).isEqualTo(expectedMondays);
    }

    @Test
    void generateSessions_skipsExistingSessions() {
        LocalDate firstMonday = LocalDate.now().datesUntil(LocalDate.now().plusDays(61))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.MONDAY)
                .findFirst().orElseThrow();

        ClassSession existing = new ClassSession();
        existing.setClassGroup(classGroup);
        existing.setInstructor(instructor);
        existing.setSessionDate(firstMonday);
        existing.setStartTime(classGroup.getStartTime());
        existing.setEndTime(classGroup.getEndTime());
        existing.setStatus(ClassSessionStatus.SCHEDULED);
        classSessionRepository.save(existing);

        classSessionScheduler.generateSessions();

        long expectedMondays = LocalDate.now().datesUntil(LocalDate.now().plusDays(61))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.MONDAY)
                .count();

        assertThat(classSessionRepository.count()).isEqualTo(expectedMondays);
    }

    @Test
    void generateSessions_skipsInactiveClassGroups() {
        classGroup.setActive(false);
        classGroupRepository.save(classGroup);

        classSessionScheduler.generateSessions();

        assertThat(classSessionRepository.count()).isZero();
    }

    @Test
    void generateSessions_doesNotAlterNonScheduledSessions() {
        LocalDate firstMonday = LocalDate.now().datesUntil(LocalDate.now().plusDays(61))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.MONDAY)
                .findFirst().orElseThrow();

        ClassSession inProgress = new ClassSession();
        inProgress.setClassGroup(classGroup);
        inProgress.setInstructor(instructor);
        inProgress.setSessionDate(firstMonday);
        inProgress.setStartTime(classGroup.getStartTime());
        inProgress.setEndTime(classGroup.getEndTime());
        inProgress.setStatus(ClassSessionStatus.IN_PROGRESS);
        classSessionRepository.save(inProgress);

        classSessionScheduler.generateSessions();

        long expectedMondays = LocalDate.now().datesUntil(LocalDate.now().plusDays(61))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.MONDAY)
                .count();

        assertThat(classSessionRepository.count()).isEqualTo(expectedMondays);
        assertThat(classSessionRepository.findById(inProgress.getId()))
                .hasValueSatisfying(s -> assertThat(s.getStatus()).isEqualTo(ClassSessionStatus.IN_PROGRESS));
    }

    @Test
    void generateSessions_handlesMultipleActiveClassGroups() {
        ClassGroup secondGroup = new ClassGroup();
        secondGroup.setStudio(studio);
        secondGroup.setInstructor(instructor);
        secondGroup.setName("Second Class");
        secondGroup.setStartTime(LocalTime.of(14, 0));
        secondGroup.setEndTime(LocalTime.of(15, 0));
        secondGroup.setCapacity(5);
        secondGroup.setWednesday(true);
        secondGroup.setActive(true);
        classGroupRepository.save(secondGroup);

        classSessionScheduler.generateSessions();

        long expectedMondays = LocalDate.now().datesUntil(LocalDate.now().plusDays(61))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.MONDAY)
                .count();
        long expectedWednesdays = LocalDate.now().datesUntil(LocalDate.now().plusDays(61))
                .filter(d -> d.getDayOfWeek() == DayOfWeek.WEDNESDAY)
                .count();

        assertThat(classSessionRepository.count()).isEqualTo(expectedMondays + expectedWednesdays);
    }

    @Test
    void generateSessions_respects60DayWindow() {
        classGroup.setMonday(true);
        classGroup.setTuesday(true);
        classGroup.setWednesday(true);
        classGroup.setThursday(true);
        classGroup.setFriday(true);
        classGroup.setSaturday(true);
        classGroup.setSunday(true);
        classGroupRepository.save(classGroup);

        classSessionScheduler.generateSessions();

        assertThat(classSessionRepository.count()).isEqualTo(61);
    }


}
