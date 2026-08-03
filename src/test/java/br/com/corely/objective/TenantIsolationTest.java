package br.com.corely.objective;

import br.com.corely.objective.dto.ObjectiveRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private ObjectMapper objectMapper;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ObjectiveRepository objectiveRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private Objective objectiveA;
    private Objective objectiveB;

    @BeforeEach
    void setUp() {
        Studio studioA = studioRepository.save(createStudio("Studio A"));
        Studio studioB = studioRepository.save(createStudio("Studio B"));

        createAndAuthenticateUser(studioA, UserRole.INSTRUCTOR);

        Student studentA = createAndSaveStudent(studioA, "Student A");
        Student studentB = createAndSaveStudent(studioB, "Student B");

        objectiveA = createAndSaveObjective(studioA, studentA, "Objective A");
        objectiveB = createAndSaveObjective(studioB, studentB, "Objective B");

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAll_shouldOnlyReturnObjectivesFromCurrentTenant() throws Exception {
        mockMvc.perform(get("/objectives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Objective A"));
    }

    @Test
    void findById_shouldReturn200_whenObjectiveBelongsToCurrentTenant() throws Exception {
        mockMvc.perform(get("/objectives/{id}", objectiveA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Objective A"));
    }

    @Test
    void findById_shouldReturn404_whenObjectiveBelongsToOtherTenant() throws Exception {
        mockMvc.perform(get("/objectives/{id}", objectiveB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_shouldReturn404_whenObjectiveBelongsToOtherTenant() throws Exception {
        var request = new ObjectiveRequest();
        request.setStudentId(objectiveB.getStudent().getId());
        request.setTitle("Hacked Objective");
        request.setStatus(ObjectiveStatus.ACTIVE);
        request.setStartDate(LocalDate.now());

        mockMvc.perform(put("/objectives/{id}", objectiveB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn404_whenObjectiveBelongsToOtherTenant() throws Exception {
        mockMvc.perform(delete("/objectives/{id}", objectiveB.getId()))
                .andExpect(status().isNotFound());
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

    private Student createAndSaveStudent(Studio studio, String name) {
        var student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setActive(true);
        return studentRepository.save(student);
    }

    private Objective createAndSaveObjective(Studio studio, Student student, String title) {
        var objective = new Objective();
        objective.setStudio(studio);
        objective.setStudent(student);
        objective.setTitle(title);
        objective.setStatus(ObjectiveStatus.ACTIVE);
        objective.setStartDate(LocalDate.now());
        return objectiveRepository.save(objective);
    }
}

