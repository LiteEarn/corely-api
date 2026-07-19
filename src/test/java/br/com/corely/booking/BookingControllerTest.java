package br.com.corely.booking;

import br.com.corely.booking.dto.*;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class BookingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private StudioRepository studioRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private InstructorRepository instructorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Student student;
    private Instructor instructor;
    private String token;

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
    void create_shouldReturn201() throws Exception {
        var json = """
                {
                    "studioId": "%s",
                    "studentId": "%s",
                    "instructorId": "%s",
                    "classType": "Pilates",
                    "startDateTime": "%s",
                    "endDateTime": "%s",
                    "capacity": 5
                }
                """.formatted(
                studio.getId(),
                student.getId(),
                instructor.getId(),
                LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).toString(),
                LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).toString()
        );

        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/bookings/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirm_shouldReturn200() throws Exception {
        var booking = createTestBooking();

        mockMvc.perform(put("/bookings/" + booking.getId() + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void cancel_shouldReturn200() throws Exception {
        var booking = createTestBooking();

        mockMvc.perform(put("/bookings/" + booking.getId() + "/cancel")
                        .param("reason", "STUDENT")
                        .param("notes", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void markNoShow_shouldReturn200() throws Exception {
        var booking = createTestBooking();

        mockMvc.perform(put("/bookings/" + booking.getId() + "/no-show"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"));
    }

    @Test
    void markCompleted_shouldReturn200() throws Exception {
        var booking = createTestBooking();

        mockMvc.perform(put("/bookings/" + booking.getId() + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getAgenda_shouldReturn200() throws Exception {
        createTestBooking();

        var start = LocalDateTime.now().minusDays(1);
        var end = LocalDateTime.now().plusDays(10);

        mockMvc.perform(get("/bookings/agenda")
                        .param("studioId", studio.getId().toString())
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        var booking = createTestBooking();

        mockMvc.perform(delete("/bookings/" + booking.getId()))
                .andExpect(status().isNoContent());
    }

    private Booking createTestBooking() {
        var booking = new Booking();
        booking.setStudio(studio);
        booking.setStudent(student);
        booking.setInstructor(instructor);
        booking.setClassType("Pilates");
        booking.setStartDateTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        booking.setEndDateTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0));
        booking.setStatus(BookingStatus.SCHEDULED);
        booking.setCapacity(5);
        booking.setActive(true);
        return bookingRepository.save(booking);
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
}
