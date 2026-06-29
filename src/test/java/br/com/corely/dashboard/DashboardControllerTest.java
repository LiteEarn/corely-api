package br.com.corely.dashboard;

import br.com.corely.attendance.Attendance;
import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.attendance.AttendanceStatus;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionStatus;
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
    private ClassSessionRepository classSessionRepository;

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
    private ClassSession classSession;
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

        classSession = new ClassSession();
        classSession.setClassGroup(classGroup);
        classSession.setInstructor(instructor);
        classSession.setSessionDate(LocalDate.now());
        classSession.setStartTime(LocalTime.of(10, 0));
        classSession.setEndTime(LocalTime.of(11, 0));
        classSession.setStatus(ClassSessionStatus.COMPLETED);
        classSession = classSessionRepository.save(classSession);

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
        attendance.setClassSession(classSession);
        attendance.setEnrollment(enrollment);
        attendance.setStatus(AttendanceStatus.PRESENT);
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
        ClassSession oldSession = new ClassSession();
        oldSession.setClassGroup(classGroup);
        oldSession.setInstructor(instructor);
        oldSession.setSessionDate(LocalDate.now().minusDays(10));
        oldSession.setStartTime(LocalTime.of(10, 0));
        oldSession.setEndTime(LocalTime.of(11, 0));
        oldSession.setStatus(ClassSessionStatus.COMPLETED);
        oldSession = classSessionRepository.save(oldSession);

        Attendance oldAttendance = new Attendance();
        oldAttendance.setClassSession(oldSession);
        oldAttendance.setEnrollment(enrollment);
        oldAttendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(oldAttendance);

        Attendance recentAttendance = new Attendance();
        recentAttendance.setClassSession(classSession);
        recentAttendance.setEnrollment(enrollment);
        recentAttendance.setStatus(AttendanceStatus.PRESENT);
        attendanceRepository.save(recentAttendance);

        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceThisWeek").value(1))
                .andExpect(jsonPath("$.attendanceThisMonth").value(2));
    }

    @Test
    void testGetDashboardExcludesInactiveStudents() throws Exception {
        Student inactiveStudent = new Student();
        inactiveStudent.setStudio(studio);
        inactiveStudent.setFullName("Inactive Student");
        inactiveStudent.setPhone("11977777777");
        inactiveStudent.setEmail("inactive@test.com");
        inactiveStudent.setActive(false);
        inactiveStudent = studentRepository.save(inactiveStudent);

        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStudents").value(1));
    }

    @Test
    void testGetDashboardExcludesInactiveInstructors() throws Exception {
        Instructor inactiveInstructor = new Instructor();
        inactiveInstructor.setStudio(studio);
        inactiveInstructor.setFullName("Inactive Instructor");
        inactiveInstructor.setEmail("inactive.instructor@test.com");
        inactiveInstructor.setPhone("11966666666");
        inactiveInstructor.setSpecialty("Pilates");
        inactiveInstructor.setActive(false);
        inactiveInstructor = instructorRepository.save(inactiveInstructor);

        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeInstructors").value(1));
    }

    @Test
    void testGetDashboardExcludesInactiveClassGroups() throws Exception {
        ClassGroup inactiveClassGroup = new ClassGroup();
        inactiveClassGroup.setStudio(studio);
        inactiveClassGroup.setInstructor(instructor);
        inactiveClassGroup.setName("Inactive Class");
        inactiveClassGroup.setDescription("Inactive Description");
        inactiveClassGroup.setStartTime(LocalTime.of(14, 0));
        inactiveClassGroup.setEndTime(LocalTime.of(15, 0));
        inactiveClassGroup.setCapacity(15);
        inactiveClassGroup.setMonday(true);
        inactiveClassGroup.setActive(false);
        inactiveClassGroup = classGroupRepository.save(inactiveClassGroup);

        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeClassGroups").value(1));
    }

    @Test
    void testGetDashboardExcludesInactiveEnrollments() throws Exception {
        Enrollment inactiveEnrollment = new Enrollment();
        inactiveEnrollment.setStudio(studio);
        inactiveEnrollment.setStudent(student);
        inactiveEnrollment.setClassGroup(classGroup);
        inactiveEnrollment.setEnrollmentDate(LocalDate.now());
        inactiveEnrollment.setActive(false);
        inactiveEnrollment = enrollmentRepository.save(inactiveEnrollment);

        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnrollments").value(1));
    }

    @Test
    void testGetDashboardOccupancyRateUsesOnlyActiveEntities() throws Exception {
        // Create inactive class group with capacity - should not be counted in totalCapacity
        ClassGroup inactiveClassGroup = new ClassGroup();
        inactiveClassGroup.setStudio(studio);
        inactiveClassGroup.setInstructor(instructor);
        inactiveClassGroup.setName("Inactive Class for Occupancy");
        inactiveClassGroup.setDescription("Inactive");
        inactiveClassGroup.setStartTime(LocalTime.of(16, 0));
        inactiveClassGroup.setEndTime(LocalTime.of(17, 0));
        inactiveClassGroup.setCapacity(50);
        inactiveClassGroup.setMonday(true);
        inactiveClassGroup.setActive(false);
        inactiveClassGroup = classGroupRepository.save(inactiveClassGroup);

        // Create inactive enrollment - should not be counted in totalEnrollments
        Enrollment inactiveEnrollment = new Enrollment();
        inactiveEnrollment.setStudio(studio);
        inactiveEnrollment.setStudent(student);
        inactiveEnrollment.setClassGroup(classGroup);
        inactiveEnrollment.setEnrollmentDate(LocalDate.now());
        inactiveEnrollment.setActive(false);
        inactiveEnrollment = enrollmentRepository.save(inactiveEnrollment);

        // Only 1 active enrollment and active class group capacity is 20
        // Occupancy rate should be (1 * 100) / 20 = 5.00
        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnrollments").value(1))
                .andExpect(jsonPath("$.activeClassGroups").value(1))
                .andExpect(jsonPath("$.occupancyRate").value(5.00));
    }

    @Test
    void testGetDashboardWithAllInactiveEntities() throws Exception {
        // Set all entities to inactive
        student.setActive(false);
        studentRepository.save(student);

        instructor.setActive(false);
        instructorRepository.save(instructor);

        classGroup.setActive(false);
        classGroupRepository.save(classGroup);

        enrollment.setActive(false);
        enrollmentRepository.save(enrollment);

        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStudents").value(0))
                .andExpect(jsonPath("$.activeInstructors").value(0))
                .andExpect(jsonPath("$.activeClassGroups").value(0))
                .andExpect(jsonPath("$.totalEnrollments").value(0))
                .andExpect(jsonPath("$.occupancyRate").value(0.00));
    }

    @Test
    void testGetDashboardWithMultipleActiveAndInactiveEntities() throws Exception {
        // Create multiple active and inactive students
        Student activeStudent2 = new Student();
        activeStudent2.setStudio(studio);
        activeStudent2.setFullName("Active Student 2");
        activeStudent2.setPhone("11955555555");
        activeStudent2.setEmail("active2@test.com");
        activeStudent2.setActive(true);
        activeStudent2 = studentRepository.save(activeStudent2);

        Student inactiveStudent2 = new Student();
        inactiveStudent2.setStudio(studio);
        inactiveStudent2.setFullName("Inactive Student 2");
        inactiveStudent2.setPhone("11944444444");
        inactiveStudent2.setEmail("inactive2@test.com");
        inactiveStudent2.setActive(false);
        inactiveStudent2 = studentRepository.save(inactiveStudent2);

        Student inactiveStudent3 = new Student();
        inactiveStudent3.setStudio(studio);
        inactiveStudent3.setFullName("Inactive Student 3");
        inactiveStudent3.setPhone("11933333333");
        inactiveStudent3.setEmail("inactive3@test.com");
        inactiveStudent3.setActive(false);
        inactiveStudent3 = studentRepository.save(inactiveStudent3);

        // Create multiple active and inactive instructors
        Instructor activeInstructor2 = new Instructor();
        activeInstructor2.setStudio(studio);
        activeInstructor2.setFullName("Active Instructor 2");
        activeInstructor2.setEmail("active.instructor@test.com");
        activeInstructor2.setPhone("11922222222");
        activeInstructor2.setSpecialty("CrossFit");
        activeInstructor2.setActive(true);
        activeInstructor2 = instructorRepository.save(activeInstructor2);

        Instructor inactiveInstructor2 = new Instructor();
        inactiveInstructor2.setStudio(studio);
        inactiveInstructor2.setFullName("Inactive Instructor 2");
        inactiveInstructor2.setEmail("inactive.instructor2@test.com");
        inactiveInstructor2.setPhone("11911111111");
        inactiveInstructor2.setSpecialty("Spinning");
        inactiveInstructor2.setActive(false);
        inactiveInstructor2 = instructorRepository.save(inactiveInstructor2);

        // Create multiple active and inactive class groups
        ClassGroup activeClassGroup2 = new ClassGroup();
        activeClassGroup2.setStudio(studio);
        activeClassGroup2.setInstructor(activeInstructor2);
        activeClassGroup2.setName("Active Class 2");
        activeClassGroup2.setDescription("Active");
        activeClassGroup2.setStartTime(LocalTime.of(18, 0));
        activeClassGroup2.setEndTime(LocalTime.of(19, 0));
        activeClassGroup2.setCapacity(25);
        activeClassGroup2.setMonday(true);
        activeClassGroup2.setActive(true);
        activeClassGroup2 = classGroupRepository.save(activeClassGroup2);

        ClassGroup inactiveClassGroup2 = new ClassGroup();
        inactiveClassGroup2.setStudio(studio);
        inactiveClassGroup2.setInstructor(inactiveInstructor2);
        inactiveClassGroup2.setName("Inactive Class 2");
        inactiveClassGroup2.setDescription("Inactive");
        inactiveClassGroup2.setStartTime(LocalTime.of(20, 0));
        inactiveClassGroup2.setEndTime(LocalTime.of(21, 0));
        inactiveClassGroup2.setCapacity(30);
        inactiveClassGroup2.setMonday(true);
        inactiveClassGroup2.setActive(false);
        inactiveClassGroup2 = classGroupRepository.save(inactiveClassGroup2);

        // Create multiple active and inactive enrollments
        Enrollment activeEnrollment2 = new Enrollment();
        activeEnrollment2.setStudio(studio);
        activeEnrollment2.setStudent(activeStudent2);
        activeEnrollment2.setClassGroup(activeClassGroup2);
        activeEnrollment2.setEnrollmentDate(LocalDate.now());
        activeEnrollment2.setActive(true);
        activeEnrollment2 = enrollmentRepository.save(activeEnrollment2);

        Enrollment inactiveEnrollment2 = new Enrollment();
        inactiveEnrollment2.setStudio(studio);
        inactiveEnrollment2.setStudent(inactiveStudent2);
        inactiveEnrollment2.setClassGroup(inactiveClassGroup2);
        inactiveEnrollment2.setEnrollmentDate(LocalDate.now());
        inactiveEnrollment2.setActive(false);
        inactiveEnrollment2 = enrollmentRepository.save(inactiveEnrollment2);

        mockMvc.perform(get("/dashboard").param("studioId", studio.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStudents").value(2))
                .andExpect(jsonPath("$.activeInstructors").value(2))
                .andExpect(jsonPath("$.activeClassGroups").value(2))
                .andExpect(jsonPath("$.totalEnrollments").value(2))
                // Total active capacity = 20 + 25 = 45, active enrollments = 2
                // Occupancy rate = (2 * 100) / 45 = 4.44
                .andExpect(jsonPath("$.occupancyRate").value(4.44));
    }
}
