package br.com.corely.comercial.scheduleslot;

import br.com.corely.comercial.schedule.Schedule;
import br.com.corely.comercial.schedule.ScheduleRepository;
import br.com.corely.comercial.scheduleslot.dto.ScheduleSlotRequest;
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
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleSlotServiceTest {

    @Autowired
    private ScheduleSlotService scheduleSlotService;

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
    private ScheduleSlot existingSlot;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        schedule = scheduleRepository.save(createSchedule("Morning Class"));
        existingSlot = scheduleSlotRepository.save(createSlot(schedule, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10));
    }

    @Test
    void create_shouldPersistSlot() {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(15);

        var response = scheduleSlotService.create(schedule.getId(), request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getScheduleId()).isEqualTo(schedule.getId());
        assertThat(response.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.getEndTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(response.getCapacity()).isEqualTo(15);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void create_shouldThrowException_whenScheduleNotFound() {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(10);

        assertThatThrownBy(() -> scheduleSlotService.create(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Schedule not found");
    }

    @Test
    void create_shouldThrowException_whenEndTimeEqualsStartTime() {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(8, 0));
        request.setCapacity(10);

        assertThatThrownBy(() -> scheduleSlotService.create(schedule.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("endTime must be greater than startTime");
    }

    @Test
    void create_shouldThrowException_whenEndTimeBeforeStartTime() {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(9, 0));
        request.setCapacity(10);

        assertThatThrownBy(() -> scheduleSlotService.create(schedule.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("endTime must be greater than startTime");
    }

    @Test
    void create_shouldThrowException_whenSlotOverlaps() {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(8, 30));
        request.setEndTime(LocalTime.of(9, 30));
        request.setCapacity(10);

        assertThatThrownBy(() -> scheduleSlotService.create(schedule.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void create_shouldAllowSlotOnDifferentDay() {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.TUESDAY);
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));
        request.setCapacity(10);

        var response = scheduleSlotService.create(schedule.getId(), request);

        assertThat(response.getId()).isNotNull();
    }

    @Test
    void create_shouldAllowSlotOnSameDayWithoutOverlap() {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
        request.setCapacity(10);

        var response = scheduleSlotService.create(schedule.getId(), request);

        assertThat(response.getId()).isNotNull();
    }

    @Test
    void create_shouldSetDefaultActiveTrue_whenNotProvided() {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.WEDNESDAY);
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));
        request.setCapacity(10);

        var response = scheduleSlotService.create(schedule.getId(), request);

        assertThat(response.getActive()).isTrue();
    }

    @Test
    void findByScheduleId_shouldReturnAllSlotsForSchedule() {
        scheduleSlotRepository.save(createSlot(schedule, DayOfWeek.WEDNESDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), 5));

        var slots = scheduleSlotService.findByScheduleId(schedule.getId());

        assertThat(slots).hasSize(2);
    }

    @Test
    void findByScheduleId_shouldThrowException_whenScheduleNotFound() {
        assertThatThrownBy(() -> scheduleSlotService.findByScheduleId(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Schedule not found");
    }

    @Test
    void findById_shouldReturnSlot() {
        var response = scheduleSlotService.findById(existingSlot.getId());

        assertThat(response).isNotNull();
        assertThat(response.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.getStartTime()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> scheduleSlotService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("ScheduleSlot not found");
    }

    @Test
    void update_shouldModifySlot() {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(7, 0));
        request.setEndTime(LocalTime.of(8, 0));
        request.setCapacity(20);

        var response = scheduleSlotService.update(existingSlot.getId(), request);

        assertThat(response.getStartTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(response.getEndTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.getCapacity()).isEqualTo(20);
    }

    @Test
    void update_shouldThrowException_whenOverlapExists() {
        scheduleSlotRepository.save(createSlot(schedule, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 0), 10));

        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(9, 30));
        request.setEndTime(LocalTime.of(10, 30));
        request.setCapacity(10);

        assertThatThrownBy(() -> scheduleSlotService.update(existingSlot.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void update_shouldAllowUpdatingWithoutOverlapConflict_whenSameSlot() {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));
        request.setCapacity(15);

        var response = scheduleSlotService.update(existingSlot.getId(), request);

        assertThat(response.getCapacity()).isEqualTo(15);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        var request = new ScheduleSlotRequest();
        request.setDayOfWeek(DayOfWeek.MONDAY);
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(9, 0));
        request.setCapacity(10);

        assertThatThrownBy(() -> scheduleSlotService.update(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_shouldSetActiveFalse() {
        scheduleSlotService.delete(existingSlot.getId());

        var entity = scheduleSlotRepository.findById(existingSlot.getId()).orElseThrow();
        assertThat(entity.getActive()).isFalse();
    }

    @Test
    void delete_shouldBeIdempotent_whenAlreadyInactive() {
        scheduleSlotService.delete(existingSlot.getId());
        scheduleSlotService.delete(existingSlot.getId());

        var entity = scheduleSlotRepository.findById(existingSlot.getId()).orElseThrow();
        assertThat(entity.getActive()).isFalse();
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> scheduleSlotService.delete(UUID.randomUUID()))
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
}
