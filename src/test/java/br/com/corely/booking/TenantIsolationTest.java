package br.com.corely.booking;

import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TimeBlockRepository timeBlockRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private Studio studioA;
    private Studio studioB;
    private Booking bookingA;
    private Booking bookingB;
    private TimeBlock timeBlockA;
    private TimeBlock timeBlockB;

    @BeforeEach
    void setUp() {
        studioA = studioRepository.save(createStudio("Studio A"));
        studioB = studioRepository.save(createStudio("Studio B"));

        createAndAuthenticateUser(studioA, UserRole.ADMIN);

        bookingA = createBookingForStudio(studioA, "Studio A");
        bookingB = createBookingForStudio(studioB, "Studio B");
        timeBlockA = createTimeBlockForStudio(studioA);
        timeBlockB = createTimeBlockForStudio(studioB);

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findById_shouldReturn200_whenBookingBelongsToCurrentTenant() throws Exception {
        mockMvc.perform(get("/bookings/{id}", bookingA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingA.getId().toString()));
    }

    @Test
    void findById_shouldReturn404_whenBookingBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/bookings/{id}", bookingB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn404_whenBookingBelongsToOtherTenant() throws Exception {
        mockMvc.perform(delete("/bookings/{id}", bookingB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirm_shouldReturn404_whenBookingBelongsToOtherTenant() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/bookings/{id}/confirm", bookingB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTimeBlock_shouldReturn204_whenTimeBlockBelongsToCurrentTenant() throws Exception {
        mockMvc.perform(delete("/bookings/time-blocks/{id}", timeBlockA.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTimeBlock_shouldReturn404_whenTimeBlockBelongsToOtherTenant() throws Exception {
        mockMvc.perform(delete("/bookings/time-blocks/{id}", timeBlockB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTimeBlocks_shouldOnlyReturnBlocksOfRequestedStudio() throws Exception {
        mockMvc.perform(get("/bookings/time-blocks")
                        .param("studioId", studioA.getId().toString())
                        .param("startDate", LocalDateTime.now().minusDays(1).toString())
                        .param("endDate", LocalDateTime.now().plusDays(60).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(timeBlockA.getId().toString()));
    }

    private User createAndAuthenticateUser(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail("admin_a@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return user;
    }

    private Studio createStudio(String name) {
        var studio = new Studio();
        studio.setName(name);
        studio.setActive(true);
        return studio;
    }

    private Booking createBookingForStudio(Studio studio, String prefix) {
        Student student = createAndSaveStudent(studio, prefix + " Student");
        Instructor instructor = createAndSaveInstructor(studio, prefix + " Instructor");

        var booking = new Booking();
        booking.setStudio(studio);
        booking.setStudent(student);
        booking.setInstructor(instructor);
        booking.setClassType("PILATES");
        booking.setStartDateTime(LocalDateTime.now().plusDays(7));
        booking.setEndDateTime(LocalDateTime.now().plusDays(7).plusHours(1));
        booking.setStatus(BookingStatus.SCHEDULED);
        booking.setCapacity(10);
        booking.setMakeUpClass(false);
        booking.setActive(true);
        return bookingRepository.save(booking);
    }

    private TimeBlock createTimeBlockForStudio(Studio studio) {
        var timeBlock = new TimeBlock();
        timeBlock.setStudio(studio);
        timeBlock.setBlockType(BlockType.HOLIDAY);
        timeBlock.setStartDate(LocalDateTime.now().plusDays(30));
        timeBlock.setEndDate(LocalDateTime.now().plusDays(30).plusHours(8));
        return timeBlockRepository.save(timeBlock);
    }

    private Student createAndSaveStudent(Studio studio, String name) {
        var student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setActive(true);
        return studentRepository.save(student);
    }

    private Instructor createAndSaveInstructor(Studio studio, String name) {
        var instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName(name);
        instructor.setEmail(name.toLowerCase().replace(" ", "") + "@test.com");
        instructor.setActive(true);
        return instructorRepository.save(instructor);
    }
}
