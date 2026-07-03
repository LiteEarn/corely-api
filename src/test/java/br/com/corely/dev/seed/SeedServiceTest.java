package br.com.corely.dev.seed;

import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.evaluation.EvaluationRepository;
import br.com.corely.evolution.EvolutionRepository;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.makeup.MakeupRequestRepository;
import br.com.corely.objective.ObjectiveRepository;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SeedServiceTest {

    @Autowired private SeedService seedService;
    @Autowired private StudioRepository studioRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private InstructorRepository instructorRepository;
    @Autowired private ClassGroupRepository classGroupRepository;
    @Autowired private ClassSessionRepository classSessionRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private MakeupRequestRepository makeupRequestRepository;
    @Autowired private ObjectiveRepository objectiveRepository;
    @Autowired private EvaluationRepository evaluationRepository;
    @Autowired private EvolutionRepository evolutionRepository;

    @BeforeEach
    void setUp() {
        seedService.clearAll();
    }

    @AfterEach
    void tearDown() {
        seedService.clearAll();
    }

    @Test
    void execute_createsAllData() {
        SeedResponse response = seedService.execute();

        assertThat(response.getStudents()).isBetween(65, 80);
        assertThat(response.getClassGroups()).isEqualTo(15);
        assertThat(response.getSessions()).isGreaterThan(0);
        assertThat(response.getAttendances()).isGreaterThan(0);
    }

    @Test
    void execute_isIdempotent() {
        seedService.execute();
        seedService.clearAll();
        SeedResponse first = seedService.execute();

        assertThat(first.getStudents()).isBetween(65, 80);
        assertThat(first.getClassGroups()).isEqualTo(15);
    }

    @Test
    void execute_createsNoOrphanRecords() {
        seedService.execute();

        assertThat(studioRepository.count()).isEqualTo(1);
        assertThat(userRepository.count()).isEqualTo(4);
        assertThat(instructorRepository.count()).isEqualTo(5);
        assertThat(classGroupRepository.count()).isEqualTo(15);
        assertThat(studentRepository.count()).isBetween(65L, 80L);
        assertThat(enrollmentRepository.count()).isGreaterThan(0);
        assertThat(objectiveRepository.count()).isGreaterThan(0);
        assertThat(evaluationRepository.count()).isGreaterThan(0);
        assertThat(evolutionRepository.count()).isGreaterThan(0);
    }

    @Test
    void seedStudentsOnly_createsStudentsAndEnrollments() {
        seedService.seedStudentsOnly();

        assertThat(studentRepository.count()).isBetween(65L, 80L);
        assertThat(enrollmentRepository.count()).isGreaterThan(0);
        assertThat(classGroupRepository.count()).isGreaterThan(0);
    }

    @Test
    void ensureDashboardData_runsWithoutError() {
        seedService.execute();
        seedService.ensureDashboardData();
        assertThat(studioRepository.count()).isEqualTo(1);
    }

    @Test
    void clearAll_removesAllData() {
        seedService.execute();
        seedService.clearAll();

        assertThat(studioRepository.count()).isZero();
        assertThat(userRepository.count()).isZero();
        assertThat(studentRepository.count()).isZero();
        assertThat(instructorRepository.count()).isZero();
        assertThat(classGroupRepository.count()).isZero();
        assertThat(classSessionRepository.count()).isZero();
        assertThat(enrollmentRepository.count()).isZero();
        assertThat(attendanceRepository.count()).isZero();
        assertThat(makeupRequestRepository.count()).isZero();
        assertThat(objectiveRepository.count()).isZero();
        assertThat(evaluationRepository.count()).isZero();
        assertThat(evolutionRepository.count()).isZero();
    }
}
