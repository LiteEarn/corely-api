package br.com.corely.dashboard;

import br.com.corely.attendance.Attendance;
import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.evaluation.Evaluation;
import br.com.corely.evaluation.EvaluationRepository;
import br.com.corely.evolution.Evolution;
import br.com.corely.evolution.EvolutionRepository;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.objective.Objective;
import br.com.corely.objective.ObjectiveRepository;
import br.com.corely.objective.ObjectiveStatus;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private ObjectiveRepository objectiveRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvolutionRepository evolutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Studio emptyStudio;
    private Student student;
    private Instructor instructor;
    private ClassGroup classGroup;
    private Enrollment enrollment;
    private User user;

    @BeforeEach
    void setUp() {
        evolutionRepository.deleteAll();
        evaluationRepository.deleteAll();
        attendanceRepository.deleteAll();
        enrollmentRepository.deleteAll();
        objectiveRepository.deleteAll();
        classGroupRepository.deleteAll();
        instructorRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        studioRepository.deleteAll();

        studio = new Studio();
        studio.setName("Test Studio");
        studio = studioRepository.save(studio);

        emptyStudio = new Studio();
        emptyStudio.setName("Empty Studio");
        emptyStudio = studioRepository.save(emptyStudio);

        student = new Student();
        student.setStudio(studio);
        student.setFullName("Test Student");
        student.setPhone("11999999999");
        student.setEmail("student@test.com");
        student.setActive(true);
        student = studentRepository.save(student);

        instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName("Test Instructor");
        instructor.setEmail("instructor@test.com");
        instructor.setPhone("11988888888");
        instructor.setSpecialty("Yoga");
        instructor.setActive(true);
        instructor = instructorRepository.save(instructor);

        classGroup = new ClassGroup();
        classGroup.setStudio(studio);
        classGroup.setInstructor(instructor);
        classGroup.setName("Test Class");
        classGroup.setDescription("Test Description");
        classGroup.setStartTime(LocalTime.of(10, 0));
        classGroup.setEndTime(LocalTime.of(11, 0));
        classGroup.setCapacity(20);
        classGroup.setMonday(true);
        classGroup.setActive(true);
        classGroup = classGroupRepository.save(classGroup);

        enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollment = enrollmentRepository.save(enrollment);

        user = new User();
        user.setName("Test User");
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);
    }

    @Test
    void testGetDashboardWithData() throws Exception {
        Attendance attendance = new Attendance();
        attendance.setStudio(studio);
        attendance.setStudent(student);
        attendance.setClassGroup(classGroup);
        attendance.setAttendanceDate(LocalDate.now());
        attendance.setPresent(true);
        attendanceRepository.save(attendance);

        Objective objective = new Objective();
        objective.setStudio(studio);
        objective.setStudent(student);
        objective.setTitle("Test Objective");
        objective.setDescription("Test Description");
        objective.setStatus(ObjectiveStatus.ACTIVE);
        objective.setStartDate(LocalDate.now());
        objectiveRepository.save(objective);

        Objective completedObjective = new Objective();
        completedObjective.setStudio(studio);
        completedObjective.setStudent(student);
        completedObjective.setTitle("Completed Objective");
        completedObjective.setDescription("Completed Description");
        completedObjective.setStatus(ObjectiveStatus.COMPLETED);
        completedObjective.setStartDate(LocalDate.now().minusDays(10));
        completedObjective.setTargetDate(LocalDate.now());
        objectiveRepository.save(completedObjective);

        Evaluation evaluation = new Evaluation();
        evaluation.setStudio(studio);
        evaluation.setStudent(student);
        evaluation.setEvaluationDate(LocalDate.now());
        evaluation.setWeight(new BigDecimal("70.5"));
        evaluation.setHeight(new BigDecimal("1.75"));
        evaluation.setObservations("Test observations");
        evaluationRepository.save(evaluation);

        Evolution evolution = new Evolution();
        evolution.setStudio(studio);
        evolution.setStudent(student);
        evolution.setEvolutionDate(LocalDate.now());
        evolution.setTitle("Test Evolution");
        evolution.setDescription("Test Description");
        evolution.setCreatedBy("Test User");
        evolutionRepository.save(evolution);

        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStudents").value(1))
                .andExpect(jsonPath("$.activeInstructors").value(1))
                .andExpect(jsonPath("$.activeClassGroups").value(1))
                .andExpect(jsonPath("$.totalEnrollments").value(1))
                .andExpect(jsonPath("$.attendanceThisWeek").value(1))
                .andExpect(jsonPath("$.attendanceThisMonth").value(1))
                .andExpect(jsonPath("$.activeObjectives").value(1))
                .andExpect(jsonPath("$.completedObjectives").value(1))
                .andExpect(jsonPath("$.evaluationsThisMonth").value(1))
                .andExpect(jsonPath("$.evolutionsThisMonth").value(1))
                .andExpect(jsonPath("$.occupancyRate").value(5.00))
                .andExpect(jsonPath("$.recentEvaluations").isArray())
                .andExpect(jsonPath("$.recentEvaluations", hasSize(1)))
                .andExpect(jsonPath("$.recentEvaluations[0].studentName").value("Test Student"))
                .andExpect(jsonPath("$.recentEvolutions").isArray())
                .andExpect(jsonPath("$.recentEvolutions", hasSize(1)))
                .andExpect(jsonPath("$.recentEvolutions[0].studentName").value("Test Student"));
    }

    @Test
    void testGetDashboardWithoutData() throws Exception {
        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStudents").value(1))
                .andExpect(jsonPath("$.activeInstructors").value(1))
                .andExpect(jsonPath("$.activeClassGroups").value(1))
                .andExpect(jsonPath("$.totalEnrollments").value(1))
                .andExpect(jsonPath("$.attendanceThisWeek").value(0))
                .andExpect(jsonPath("$.attendanceThisMonth").value(0))
                .andExpect(jsonPath("$.activeObjectives").value(0))
                .andExpect(jsonPath("$.completedObjectives").value(0))
                .andExpect(jsonPath("$.evaluationsThisMonth").value(0))
                .andExpect(jsonPath("$.evolutionsThisMonth").value(0))
                .andExpect(jsonPath("$.occupancyRate").value(5.00))
                .andExpect(jsonPath("$.recentEvaluations").isArray())
                .andExpect(jsonPath("$.recentEvaluations", hasSize(0)))
                .andExpect(jsonPath("$.recentEvolutions").isArray())
                .andExpect(jsonPath("$.recentEvolutions", hasSize(0)));
    }

    @Test
    void testGetDashboardByStudioIdEmptyStudio() throws Exception {
        mockMvc.perform(get("/dashboard").param("studioId", emptyStudio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStudents").value(0))
                .andExpect(jsonPath("$.activeInstructors").value(0))
                .andExpect(jsonPath("$.activeClassGroups").value(0))
                .andExpect(jsonPath("$.totalEnrollments").value(0))
                .andExpect(jsonPath("$.attendanceThisWeek").value(0))
                .andExpect(jsonPath("$.attendanceThisMonth").value(0))
                .andExpect(jsonPath("$.activeObjectives").value(0))
                .andExpect(jsonPath("$.completedObjectives").value(0))
                .andExpect(jsonPath("$.evaluationsThisMonth").value(0))
                .andExpect(jsonPath("$.evolutionsThisMonth").value(0))
                .andExpect(jsonPath("$.occupancyRate").value(0.00))
                .andExpect(jsonPath("$.recentEvaluations").isArray())
                .andExpect(jsonPath("$.recentEvaluations", hasSize(0)))
                .andExpect(jsonPath("$.recentEvolutions").isArray())
                .andExpect(jsonPath("$.recentEvolutions", hasSize(0)));
    }

    @Test
    void testGetDashboardByStudioIdNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/dashboard").param("studioId", nonExistentId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDashboardWithMultipleRecentItems() throws Exception {
        for (int i = 0; i < 7; i++) {
            Evaluation evaluation = new Evaluation();
            evaluation.setStudio(studio);
            evaluation.setStudent(student);
            evaluation.setEvaluationDate(LocalDate.now().minusDays(i));
            evaluation.setWeight(new BigDecimal("70.5"));
            evaluation.setHeight(new BigDecimal("1.75"));
            evaluationRepository.save(evaluation);

            Evolution evolution = new Evolution();
            evolution.setStudio(studio);
            evolution.setStudent(student);
            evolution.setEvolutionDate(LocalDate.now().minusDays(i));
            evolution.setTitle("Evolution " + i);
            evolution.setDescription("Description " + i);
            evolution.setCreatedBy("Test User");
            evolutionRepository.save(evolution);
        }

        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentEvaluations").isArray())
                .andExpect(jsonPath("$.recentEvaluations", hasSize(5)))
                .andExpect(jsonPath("$.recentEvolutions").isArray())
                .andExpect(jsonPath("$.recentEvolutions", hasSize(5)));
    }

    @Test
    void testGetDashboardOccupancyRateZeroCapacity() throws Exception {
        classGroup.setCapacity(0);
        classGroupRepository.save(classGroup);

        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupancyRate").value(0.00));
    }

    @Test
    void testGetDashboardAttendanceDateFilters() throws Exception {
        Attendance oldAttendance = new Attendance();
        oldAttendance.setStudio(studio);
        oldAttendance.setStudent(student);
        oldAttendance.setClassGroup(classGroup);
        oldAttendance.setAttendanceDate(LocalDate.now().minusDays(10));
        oldAttendance.setPresent(true);
        attendanceRepository.save(oldAttendance);

        Attendance recentAttendance = new Attendance();
        recentAttendance.setStudio(studio);
        recentAttendance.setStudent(student);
        recentAttendance.setClassGroup(classGroup);
        recentAttendance.setAttendanceDate(LocalDate.now());
        recentAttendance.setPresent(true);
        attendanceRepository.save(recentAttendance);

        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceThisWeek").value(1))
                .andExpect(jsonPath("$.attendanceThisMonth").value(1));
    }
}
