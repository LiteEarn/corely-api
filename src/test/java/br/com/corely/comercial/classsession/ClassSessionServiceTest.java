package br.com.corely.comercial.classsession;

import br.com.corely.comercial.schedule.Schedule;
import br.com.corely.comercial.schedule.ScheduleRepository;
import br.com.corely.comercial.scheduleslot.ScheduleSlot;
import br.com.corely.comercial.scheduleslot.ScheduleSlotRepository;
import br.com.corely.comercial.classsession.dto.ClassSessionRequest;
import br.com.corely.comercial.classsession.dto.SessionStatusDto;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClassSessionServiceTest {

    @Autowired
    private ClassSessionService classSessionService;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Schedule schedule;
    private ScheduleSlot slot;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        schedule = scheduleRepository.save(createSchedule("Morning Class"));
        slot = scheduleSlotRepository.save(createSlot(schedule, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));
    }

    @Test
    void create_shouldPersistSession() {
        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        var response = classSessionService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getScheduleSlotId()).isEqualTo(slot.getId());
        assertThat(response.getSessionDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(response.getStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.getEndTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.getCapacity()).isEqualTo(10);
        assertThat(response.getBookedCount()).isZero();
        assertThat(response.getStatus()).isEqualTo(SessionStatusDto.SCHEDULED);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void create_shouldThrowException_whenScheduleSlotNotFound() {
        var request = new ClassSessionRequest();
        request.setScheduleSlotId(UUID.randomUUID());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        assertThatThrownBy(() -> classSessionService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("ScheduleSlot not found");
    }

    @Test
    void create_shouldThrowException_whenDuplicate() {
        classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        assertThatThrownBy(() -> classSessionService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void create_shouldAllowDifferentDate() {
        classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 2));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        var response = classSessionService.create(request);

        assertThat(response.getId()).isNotNull();
    }

    @Test
    void create_shouldThrowException_whenEndTimeEqualsStartTime() {
        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(8, 0));

        assertThatThrownBy(() -> classSessionService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("endTime must be greater than startTime");
    }

    @Test
    void create_shouldThrowException_whenEndTimeBeforeStartTime() {
        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(9, 0));

        assertThatThrownBy(() -> classSessionService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("endTime must be greater than startTime");
    }

    @Test
    void findById_shouldReturnSession() {
        var saved = classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        var response = classSessionService.findById(saved.getId());

        assertThat(response).isNotNull();
        assertThat(response.getSessionDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> classSessionService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("ClassSession not found");
    }

    @Test
    void findAll_shouldReturnPagedResults() {
        classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));
        classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 2),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        var page = classSessionService.findAll(null, null, org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void update_shouldModifySession() {
        var saved = classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(7, 0));
        request.setEndTime(LocalTime.of(8, 0));

        var response = classSessionService.update(saved.getId(), request);

        assertThat(response.getStartTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(response.getEndTime()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void update_shouldThrowException_whenStatusNotScheduled() {
        var saved = classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));
        saved.setStatus(SessionStatus.FINISHED);
        classSessionRepository.save(saved);

        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        assertThatThrownBy(() -> classSessionService.update(saved.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot modify");
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        var request = new ClassSessionRequest();
        request.setScheduleSlotId(slot.getId());
        request.setSessionDate(LocalDate.of(2026, 8, 1));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));

        assertThatThrownBy(() -> classSessionService.update(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_shouldSetActiveFalse() {
        var saved = classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        classSessionService.delete(saved.getId());

        var entity = classSessionRepository.findById(saved.getId()).orElseThrow();
        assertThat(entity.getActive()).isFalse();
    }

    @Test
    void delete_shouldBeIdempotent_whenAlreadyInactive() {
        var saved = classSessionRepository.save(createSession(slot, LocalDate.of(2026, 8, 1),
                LocalTime.of(8, 0), LocalTime.of(9, 0)));

        classSessionService.delete(saved.getId());
        classSessionService.delete(saved.getId());

        var entity = classSessionRepository.findById(saved.getId()).orElseThrow();
        assertThat(entity.getActive()).isFalse();
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> classSessionService.delete(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "_" + studio.getId() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Studio createStudio(String name) {
        var studio = new Studio();
        studio.setName(name);
        studio.setActive(true);
        return studio;
    }

    private Schedule createSchedule(String name) {
        var schedule = new Schedule();
        schedule.setStudio(studio);
        schedule.setName(name);
        schedule.setActive(true);
        return schedule;
    }

    private ScheduleSlot createSlot(Schedule schedule, DayOfWeek dayOfWeek,
                                    LocalTime startTime, LocalTime endTime, int capacity) {
        var slot = new ScheduleSlot();
        slot.setStudio(studio);
        slot.setSchedule(schedule);
        slot.setDayOfWeek(dayOfWeek);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setCapacity(capacity);
        slot.setActive(true);
        return slot;
    }

    private ClassSession createSession(ScheduleSlot slot, LocalDate date,
                                       LocalTime startTime, LocalTime endTime) {
        var session = new ClassSession();
        session.setStudio(studio);
        session.setScheduleSlot(slot);
        session.setSessionDate(date);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setCapacity(slot.getCapacity());
        session.setBookedCount(0);
        session.setStatus(SessionStatus.SCHEDULED);
        session.setActive(true);
        return session;
    }
}
