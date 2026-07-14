package br.com.corely.comercial.schedule;

import br.com.corely.comercial.schedule.dto.ScheduleRequest;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleServiceTest {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Schedule existingSchedule;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Test Studio"));
        authenticateAs(studio, UserRole.ADMIN);

        existingSchedule = scheduleRepository.save(createSchedule("Morning Class"));
    }

    @Test
    void create_shouldPersistSchedule() {
        var request = new ScheduleRequest();
        request.setName("Evening Class");
        request.setDescription("Evening class description");

        var response = scheduleService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Evening Class");
        assertThat(response.getDescription()).isEqualTo("Evening class description");
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void create_shouldThrowException_whenNameAlreadyExistsInSameStudio() {
        var request = new ScheduleRequest();
        request.setName("Morning Class");

        assertThatThrownBy(() -> scheduleService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Schedule name already exists");
    }

    @Test
    void create_shouldAllowSameNameInDifferentStudio() {
        var otherStudio = studioRepository.save(createStudio("Other Studio"));
        authenticateAs(otherStudio, UserRole.ADMIN);

        var request = new ScheduleRequest();
        request.setName("Morning Class");

        var response = scheduleService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Morning Class");
    }

    @Test
    void create_shouldSetDefaultActiveTrue_whenNotProvided() {
        var request = new ScheduleRequest();
        request.setName("New Schedule");

        var response = scheduleService.create(request);

        assertThat(response.getActive()).isTrue();
    }

    @Test
    void findById_shouldReturnSchedule() {
        var response = scheduleService.findById(existingSchedule.getId());

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Morning Class");
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> scheduleService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Schedule not found");
    }

    @Test
    void findAll_shouldReturnPagedResults() {
        var page = scheduleService.findAll(null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findAll_shouldFilterByName() {
        var page = scheduleService.findAll("Morning", null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Morning Class");
    }

    @Test
    void findAll_shouldFilterByActive() {
        var page = scheduleService.findAll(null, true, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findAll_shouldReturnEmpty_whenNoMatch() {
        var page = scheduleService.findAll("Nonexistent", null, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void update_shouldModifySchedule() {
        var request = new ScheduleRequest();
        request.setName("Updated Class");
        request.setDescription("Updated description");

        var response = scheduleService.update(existingSchedule.getId(), request);

        assertThat(response.getName()).isEqualTo("Updated Class");
        assertThat(response.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void update_shouldThrowException_whenNameAlreadyExistsInSameStudio() {
        var another = scheduleRepository.save(createSchedule("Another Class"));

        var request = new ScheduleRequest();
        request.setName("Another Class");

        assertThatThrownBy(() -> scheduleService.update(existingSchedule.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Schedule name already exists");
    }

    @Test
    void update_shouldAllowSameNameForSameSchedule() {
        var request = new ScheduleRequest();
        request.setName("Morning Class");

        var response = scheduleService.update(existingSchedule.getId(), request);

        assertThat(response.getName()).isEqualTo("Morning Class");
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        var request = new ScheduleRequest();
        request.setName("Nonexistent");

        assertThatThrownBy(() -> scheduleService.update(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_shouldSetActiveFalse() {
        scheduleService.delete(existingSchedule.getId());

        var entity = scheduleRepository.findById(existingSchedule.getId()).orElseThrow();
        assertThat(entity.getActive()).isFalse();
    }

    @Test
    void delete_shouldBeIdempotent_whenAlreadyInactive() {
        scheduleService.delete(existingSchedule.getId());

        scheduleService.delete(existingSchedule.getId());

        var entity = scheduleRepository.findById(existingSchedule.getId()).orElseThrow();
        assertThat(entity.getActive()).isFalse();
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> scheduleService.delete(UUID.randomUUID()))
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
}
