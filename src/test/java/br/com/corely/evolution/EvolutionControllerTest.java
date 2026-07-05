package br.com.corely.evolution;

import br.com.corely.evaluation.Evaluation;
import br.com.corely.evaluation.EvaluationRepository;
import br.com.corely.evolution.dto.EvolutionRequest;
import br.com.corely.evolution.dto.EvolutionResponse;
import br.com.corely.objective.Objective;
import br.com.corely.objective.ObjectiveRepository;
import br.com.corely.objective.ObjectiveStatus;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EvolutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EvolutionRepository evolutionRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ObjectiveRepository objectiveRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    private Studio studio;
    private Student student;
    private Objective objective;
    private Evaluation evaluation;

    @BeforeEach
    void setUp() {
        evolutionRepository.deleteAll();
        evaluationRepository.deleteAll();
        objectiveRepository.deleteAll();
        studentRepository.deleteAll();
        studioRepository.deleteAll();

        studio = new Studio();
        studio.setName("Test Studio");
        studio = studioRepository.save(studio);

        student = new Student();
        student.setStudio(studio);
        student.setFullName("Test Student");
        student.setActive(true);
        student = studentRepository.save(student);

        objective = new Objective();
        objective.setStudio(studio);
        objective.setStudent(student);
        objective.setTitle("Test Objective");
        objective.setDescription("Test Description");
        objective.setStatus(ObjectiveStatus.ACTIVE);
        objective.setStartDate(LocalDate.now());
        objective = objectiveRepository.save(objective);

        evaluation = new Evaluation();
        evaluation.setStudio(studio);
        evaluation.setStudent(student);
        evaluation.setEvaluationDate(LocalDate.now());
        evaluation.setWeight(new BigDecimal("70.5"));
        evaluation.setHeight(new BigDecimal("1.75"));
        evaluation = evaluationRepository.save(evaluation);
    }

    @Test
    @WithMockUser
    void testCreateEvolution() throws Exception {
        EvolutionRequest request = new EvolutionRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setObjectiveId(objective.getId());
        request.setEvaluationId(evaluation.getId());
        request.setEvolutionDate(LocalDate.now());
        request.setTitle("Test Evolution");
        request.setDescription("Test Description");

        mockMvc.perform(post("/evolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.studioId").value(studio.getId().toString()))
                .andExpect(jsonPath("$.studentId").value(student.getId().toString()))
                .andExpect(jsonPath("$.studentName").value(student.getFullName()))
                .andExpect(jsonPath("$.objectiveId").value(objective.getId().toString()))
                .andExpect(jsonPath("$.objectiveTitle").value(objective.getTitle()))
                .andExpect(jsonPath("$.evaluationId").value(evaluation.getId().toString()))
                .andExpect(jsonPath("$.title").value("Test Evolution"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.createdBy").value("user"));
    }

    @Test
    @WithMockUser
    void testCreateEvolutionWithoutObjectiveAndEvaluation() throws Exception {
        EvolutionRequest request = new EvolutionRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setEvolutionDate(LocalDate.now());
        request.setTitle("Test Evolution");
        request.setDescription("Test Description");

        mockMvc.perform(post("/evolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.objectiveId").isEmpty())
                .andExpect(jsonPath("$.evaluationId").isEmpty());
    }

    @Test
    @WithMockUser
    void testCreateEvolutionValidation() throws Exception {
        EvolutionRequest request = new EvolutionRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setEvolutionDate(LocalDate.now());
        request.setTitle("ab"); // Less than 3 characters
        request.setDescription("Test Description");

        mockMvc.perform(post("/evolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testFindAllEvolutions() throws Exception {
        Evolution evolution = new Evolution();
        evolution.setStudio(studio);
        evolution.setStudent(student);
        evolution.setObjective(objective);
        evolution.setEvaluation(evaluation);
        evolution.setEvolutionDate(LocalDate.now());
        evolution.setTitle("Test Evolution");
        evolution.setDescription("Test Description");
        evolution.setCreatedBy("Test User");
        evolutionRepository.save(evolution);

        mockMvc.perform(get("/evolutions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Test Evolution"));
    }

    @Test
    @WithMockUser
    void testFindEvolutionById() throws Exception {
        Evolution evolution = new Evolution();
        evolution.setStudio(studio);
        evolution.setStudent(student);
        evolution.setObjective(objective);
        evolution.setEvaluation(evaluation);
        evolution.setEvolutionDate(LocalDate.now());
        evolution.setTitle("Test Evolution");
        evolution.setDescription("Test Description");
        evolution.setCreatedBy("Test User");
        evolution = evolutionRepository.save(evolution);

        mockMvc.perform(get("/evolutions/" + evolution.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(evolution.getId().toString()))
                .andExpect(jsonPath("$.title").value("Test Evolution"));
    }

    @Test
    @WithMockUser
    void testFindEvolutionByIdNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/evolutions/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void testFindByStudentId() throws Exception {
        Evolution evolution1 = new Evolution();
        evolution1.setStudio(studio);
        evolution1.setStudent(student);
        evolution1.setEvolutionDate(LocalDate.now());
        evolution1.setTitle("Evolution 1");
        evolution1.setDescription("Description 1");
        evolution1.setCreatedBy("Test User");
        evolutionRepository.save(evolution1);

        Evolution evolution2 = new Evolution();
        evolution2.setStudio(studio);
        evolution2.setStudent(student);
        evolution2.setEvolutionDate(LocalDate.now().plusDays(1));
        evolution2.setTitle("Evolution 2");
        evolution2.setDescription("Description 2");
        evolution2.setCreatedBy("Test User");
        evolutionRepository.save(evolution2);

        mockMvc.perform(get("/evolutions?studentId=" + student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser
    void testFindByObjectiveId() throws Exception {
        Evolution evolution1 = new Evolution();
        evolution1.setStudio(studio);
        evolution1.setStudent(student);
        evolution1.setObjective(objective);
        evolution1.setEvolutionDate(LocalDate.now());
        evolution1.setTitle("Evolution 1");
        evolution1.setDescription("Description 1");
        evolution1.setCreatedBy("Test User");
        evolutionRepository.save(evolution1);

        mockMvc.perform(get("/evolutions?objectiveId=" + objective.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].objectiveId").value(objective.getId().toString()));
    }

    @Test
    @WithMockUser
    void testFindByEvolutionDateBetween() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(5);

        Evolution evolution1 = new Evolution();
        evolution1.setStudio(studio);
        evolution1.setStudent(student);
        evolution1.setEvolutionDate(LocalDate.now());
        evolution1.setTitle("Evolution 1");
        evolution1.setDescription("Description 1");
        evolution1.setCreatedBy("Test User");
        evolutionRepository.save(evolution1);

        mockMvc.perform(get("/evolutions")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser
    void testUpdateEvolution() throws Exception {
        Evolution evolution = new Evolution();
        evolution.setStudio(studio);
        evolution.setStudent(student);
        evolution.setObjective(objective);
        evolution.setEvolutionDate(LocalDate.now());
        evolution.setTitle("Original Title");
        evolution.setDescription("Original Description");
        evolution.setCreatedBy("Test User");
        evolution = evolutionRepository.save(evolution);

        EvolutionRequest request = new EvolutionRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setObjectiveId(objective.getId());
        request.setEvolutionDate(LocalDate.now());
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");

        mockMvc.perform(put("/evolutions/" + evolution.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(evolution.getId().toString()))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"));
    }

    @Test
    @WithMockUser
    void testUpdateEvolutionNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        EvolutionRequest request = new EvolutionRequest();
        request.setStudioId(studio.getId());
        request.setStudentId(student.getId());
        request.setEvolutionDate(LocalDate.now());
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");

        mockMvc.perform(put("/evolutions/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void testDeleteEvolution() throws Exception {
        Evolution evolution = new Evolution();
        evolution.setStudio(studio);
        evolution.setStudent(student);
        evolution.setEvolutionDate(LocalDate.now());
        evolution.setTitle("Test Evolution");
        evolution.setDescription("Test Description");
        evolution.setCreatedBy("Test User");
        evolution = evolutionRepository.save(evolution);

        mockMvc.perform(delete("/evolutions/" + evolution.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/evolutions/" + evolution.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void testDeleteEvolutionNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/evolutions/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/evolutions"))
                .andExpect(status().isForbidden());
    }
}
