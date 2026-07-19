package br.com.corely.booking;

import br.com.corely.booking.dto.BookingRequest;
import br.com.corely.booking.dto.BookingResponse;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceTest {

    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private StudioRepository studioRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private InstructorRepository instructorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Student student;
    private Instructor instructor;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        instructorRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        studioRepository.deleteAll();

        studio = new Studio();
        studio.setName("Test Studio");
        studio.setActive(true);
        studio = studioRepository.save(studio);

        student = new Student();
        student.setStudio(studio);
        student.setFullName("Test Student");
        student.setActive(true);
        student = studentRepository.save(student);

        instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName("Test Instructor");
        instructor.setEmail("instr@test.com");
        instructor.setActive(true);
        instructor = instructorRepository.save(instructor);

        authenticateAs(studio, UserRole.ADMIN);
    }

    @Test
    void create_shouldPersistBooking() {
        var request = buildRequest();

        var response = bookingService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStudentName()).isEqualTo("Test Student");
        assertThat(response.getInstructorName()).isEqualTo("Test Instructor");
        assertThat(response.getClassType()).isEqualTo("Pilates");
        assertThat(response.getStatus()).isEqualTo(BookingStatus.SCHEDULED);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void create_shouldThrowException_whenEndBeforeStart() {
        var request = buildRequest();
        request.setStartDateTime(LocalDateTime.now().plusDays(2));
        request.setEndDateTime(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void create_shouldThrowException_whenStudentConflict() {
        var request = buildRequest();
        bookingService.create(request);

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Student already has a booking");
    }

    @Test
    void create_shouldThrowException_whenInstructorConflict() {
        var request = buildRequest();
        bookingService.create(request);

        var otherStudent = new Student();
        otherStudent.setStudio(studio);
        otherStudent.setFullName("Other Student");
        otherStudent.setActive(true);
        otherStudent = studentRepository.save(otherStudent);

        var otherRequest = buildRequest();
        otherRequest.setStudentId(otherStudent.getId());

        assertThatThrownBy(() -> bookingService.create(otherRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Instructor already has a booking");
    }

    @Test
    void create_shouldThrowException_whenStudioNotFound() {
        var request = buildRequest();
        request.setStudioId(UUID.randomUUID());

        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Studio not found");
    }

    @Test
    void findById_shouldReturnBooking() {
        var created = bookingService.create(buildRequest());

        var response = bookingService.findById(created.getId());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(created.getId());
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> bookingService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirm_shouldUpdateStatus() {
        var created = bookingService.create(buildRequest());

        var response = bookingService.confirm(created.getId());

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cancel_shouldUpdateStatusAndDeactivate() {
        var created = bookingService.create(buildRequest());

        var response = bookingService.cancel(created.getId(), CancellationReason.STUDENT, "Test reason");

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.getActive()).isFalse();
        assertThat(response.getCancellationReason()).isEqualTo(CancellationReason.STUDENT);
        assertThat(response.getCancellationNotes()).isEqualTo("Test reason");
    }

    @Test
    void cancel_shouldThrowException_whenAlreadyCancelled() {
        var created = bookingService.create(buildRequest());
        bookingService.cancel(created.getId(), CancellationReason.STUDENT, "");

        assertThatThrownBy(() -> bookingService.cancel(created.getId(), CancellationReason.STUDENT, ""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void reschedule_shouldUpdateTimes() {
        var request = buildRequest();
        request.setStartDateTime(LocalDateTime.now().plusDays(5).withHour(10).withMinute(0));
        request.setEndDateTime(LocalDateTime.now().plusDays(5).withHour(11).withMinute(0));
        var created = bookingService.create(request);
        var newStart = LocalDateTime.now().plusDays(6).withHour(14).withMinute(0);
        var newEnd = newStart.plusHours(1);

        var response = bookingService.reschedule(created.getId(), newStart, newEnd);

        assertThat(response.getStartDateTime()).isEqualTo(newStart);
        assertThat(response.getEndDateTime()).isEqualTo(newEnd);
    }

    @Test
    void markNoShow_shouldUpdateStatus() {
        var created = bookingService.create(buildRequest());

        var response = bookingService.markNoShow(created.getId());

        assertThat(response.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
    }

    @Test
    void markCompleted_shouldUpdateStatus() {
        var created = bookingService.create(buildRequest());

        var response = bookingService.markCompleted(created.getId());

        assertThat(response.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    void delete_shouldRemoveBooking() {
        var created = bookingService.create(buildRequest());

        bookingService.delete(created.getId());

        assertThat(bookingRepository.findById(created.getId())).isNotPresent();
    }

    @Test
    void findAgenda_shouldReturnBookingsInRange() {
        bookingService.create(buildRequest());
        var start = LocalDateTime.now().minusDays(1);
        var end = LocalDateTime.now().plusDays(10);

        var results = bookingService.findAgenda(studio.getId(), start, end);

        assertThat(results).isNotEmpty();
    }

    @Test
    void getDashboardMetrics_shouldReturnMetrics() {
        bookingService.create(buildRequest());

        var metrics = bookingService.getDashboardMetrics(studio.getId());

        assertThat(metrics.getTodayClasses()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getWeekClasses()).isGreaterThanOrEqualTo(0);
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "_" + UUID.randomUUID() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private BookingRequest buildRequest() {
        var request = new BookingRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setInstructorId(instructor.getId());
        request.setClassType("Pilates");
        request.setStartDateTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        request.setEndDateTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0));
        request.setCapacity(5);
        return request;
    }
}
