package br.com.corely.attendance;

import br.com.corely.attendance.dto.AttendanceRequest;
import br.com.corely.attendance.dto.SessionBulkAttendanceRequest;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
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

import java.time.LocalDate;
import java.time.LocalTime;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudioRepository studioRepository;

    private Studio studio;
    private Instructor instructor;
    private ClassGroup classGroup;
    private Student student;
    private Enrollment enrollment;
    private ClassSession session;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        enrollmentRepository.deleteAll();
        classSessionRepository.deleteAll();
        classGroupRepository.deleteAll();
        instructorRepository.deleteAll();
        studentRepository.deleteAll();
        studioRepository.deleteAll();

        studio = new Studio();
        studio.setName("Test Studio");
        studio = studioRepository.save(studio);

        instructor = new Instructor();
        instructor.setStudio(studio);
        instructor.setFullName("Test Instructor");
        instructor.setEmail("instructor@test.com");
        instructor.setActive(true);
        instructor = instructorRepository.save(instructor);

        classGroup = new ClassGroup();
        classGroup.setStudio(studio);
        classGroup.setInstructor(instructor);
        classGroup.setName("Test Class Group");
        classGroup.setStartTime(LocalTime.of(10, 0));
        classGroup.setEndTime(LocalTime.of(11, 0));
        classGroup.setCapacity(10);
        classGroup.setMonday(true);
        classGroup.setActive(true);
        classGroup = classGroupRepository.save(classGroup);

        student = new Student();
        student.setStudio(studio);
        student.setFullName("Test Student");
        student.setEmail("student@test.com");
        student.setActive(true);
        student = studentRepository.save(student);

        enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setActive(true);
        enrollment = enrollmentRepository.save(enrollment);

        session = new ClassSession();
        session.setClassGroup(classGroup);
        session.setInstructor(instructor);
        session.setSessionDate(LocalDate.now());
        session.setStartTime(LocalTime.of(10, 0));
        session.setEndTime(LocalTime.of(11, 0));
        session.setStatus(ClassSessionStatus.IN_PROGRESS);
        session = classSessionRepository.save(session);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findByClassGroupAndDate_returnsAttendances() throws Exception {
        AttendanceRequest request = new AttendanceRequest(
                enrollment.getId(),
                AttendanceStatus.PRESENT,
                "Test notes"
        );

        mockMvc.perform(post("/class-sessions/{sessionId}/attendance", session.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/attendance/class-group/{classGroupId}/date/{date}",
                        classGroup.getId(), LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].classSessionId").value(session.getId().toString()))
                .andExpect(jsonPath("$[0].enrollmentId").value(enrollment.getId().toString()))
                .andExpect(jsonPath("$[0].studentName").value(student.getFullName()))
                .andExpect(jsonPath("$[0].status").value("PRESENT"))
                .andExpect(jsonPath("$[0].notes").value("Test notes"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findByClassGroupAndDate_whenNoAttendance_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/attendance/class-group/{classGroupId}/date/{date}",
                        classGroup.getId(), LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findByClassGroupAndDate_whenClassGroupNotFound_returnsNotFound() throws Exception {
        mockMvc.perform(get("/attendance/class-group/{classGroupId}/date/{date}",
                        java.util.UUID.randomUUID(), LocalDate.now().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveSessionAttendances_createsAttendance() throws Exception {
        SessionBulkAttendanceRequest request = new SessionBulkAttendanceRequest();
        var item = new SessionBulkAttendanceRequest.AttendanceItem();
        item.setEnrollmentId(enrollment.getId());
        item.setStatus(AttendanceStatus.PRESENT);
        request.setAttendances(List.of(item));

        mockMvc.perform(put("/class-sessions/{sessionId}/attendance", session.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].classSessionId").value(session.getId().toString()))
                .andExpect(jsonPath("$[0].enrollmentId").value(enrollment.getId().toString()))
                .andExpect(jsonPath("$[0].studentName").value(student.getFullName()))
                .andExpect(jsonPath("$[0].status").value("PRESENT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveSessionAttendances_multipleStudents() throws Exception {
        Student student2 = new Student();
        student2.setStudio(studio);
        student2.setFullName("Second Student");
        student2.setEmail("second@test.com");
        student2.setActive(true);
        student2 = studentRepository.save(student2);

        Enrollment enrollment2 = new Enrollment();
        enrollment2.setStudio(studio);
        enrollment2.setStudent(student2);
        enrollment2.setClassGroup(classGroup);
        enrollment2.setEnrollmentDate(LocalDate.now());
        enrollment2.setActive(true);
        enrollment2 = enrollmentRepository.save(enrollment2);

        SessionBulkAttendanceRequest request = new SessionBulkAttendanceRequest();
        var item1 = new SessionBulkAttendanceRequest.AttendanceItem();
        item1.setEnrollmentId(enrollment.getId());
        item1.setStatus(AttendanceStatus.PRESENT);
        var item2 = new SessionBulkAttendanceRequest.AttendanceItem();
        item2.setEnrollmentId(enrollment2.getId());
        item2.setStatus(AttendanceStatus.ABSENT);
        request.setAttendances(List.of(item1, item2));

        mockMvc.perform(put("/class-sessions/{sessionId}/attendance", session.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status").value("PRESENT"))
                .andExpect(jsonPath("$[1].status").value("ABSENT"));
    }
}
