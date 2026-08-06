package br.com.corely.finance.installment;

import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.studentplan.dto.StudentPlanRequest;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração do endpoint de parcelas (EPIC-03-S02).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReceivableInstallmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ReceivableInstallmentRepository installmentRepository;

    @Autowired
    private br.com.corely.comercial.contract.ContractApplicationService contractApplicationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Student student;

    @BeforeEach
    void setUp() {
        installmentRepository.deleteAll();
        planRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        studioRepository.deleteAll();

        studio = studioRepository.save(createStudio("Installment Studio"));
        student = createAndSaveStudent(studio, "Installment Student");
        createAndAuthenticateUser(studio, UserRole.ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enroll_shouldGenerateInstallmentsAutomatically() throws Exception {
        var plan = planRepository.save(createPlan("Premium", BigDecimal.valueOf(100), 60));

        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.of(2026, 1, 15));
        contractApplicationService.enroll(request);

        mockMvc.perform(get("/finance/installments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].installmentNumber").value(1))
                .andExpect(jsonPath("$.content[1].installmentNumber").value(2))
                .andExpect(jsonPath("$.content[0].studentName").value("Installment Student"));
    }

    @Test
    void findAll_shouldFilterByStatus() throws Exception {
        var plan = planRepository.save(createPlan("Premium", BigDecimal.valueOf(100), 30));
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.of(2026, 1, 15));
        contractApplicationService.enroll(request);

        mockMvc.perform(get("/finance/installments").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    @Test
    void findById_shouldReturnInstallment() throws Exception {
        var plan = planRepository.save(createPlan("Premium", BigDecimal.valueOf(100), 30));
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.of(2026, 1, 15));
        contractApplicationService.enroll(request);

        var installment = installmentRepository.findAll().get(0);

        mockMvc.perform(get("/finance/installments/{id}", installment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(installment.getId().toString()))
                .andExpect(jsonPath("$.installmentNumber").value(1));
    }

    @Test
    void findAll_withInvertedDateRange_shouldReturn400() throws Exception {
        mockMvc.perform(get("/finance/installments")
                        .param("dueDateFrom", "2026-09-10")
                        .param("dueDateTo", "2026-09-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_shouldOnlyReturnCurrentTenantInstallments() throws Exception {
        var plan = planRepository.save(createPlan("Premium", BigDecimal.valueOf(100), 60));
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.of(2026, 1, 15));
        contractApplicationService.enroll(request);

        Studio otherStudio = studioRepository.save(createStudio("Other Installment Studio"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherPlan = planRepository.save(createPlan("Other Plan", BigDecimal.valueOf(200), 30));
        var otherRequest = new StudentPlanRequest();
        otherRequest.setStudentId(otherStudent.getId());
        otherRequest.setPlanId(otherPlan.getId());
        otherRequest.setStartDate(LocalDate.of(2026, 1, 15));
        createAndAuthenticateUser(otherStudio, UserRole.ADMIN);
        contractApplicationService.enroll(otherRequest);
        createAndAuthenticateUser(studio, UserRole.ADMIN);

        mockMvc.perform(get("/finance/installments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].studentName").value("Installment Student"));
    }

    @Test
    void findById_shouldReturn404WhenInstallmentBelongsToOtherTenant() throws Exception {
        Studio otherStudio = studioRepository.save(createStudio("Other Installment Studio"));
        Student otherStudent = createAndSaveStudent(otherStudio, "Other Student");
        var otherPlan = planRepository.save(createPlan("Other Plan", BigDecimal.valueOf(200), 30));
        var otherRequest = new StudentPlanRequest();
        otherRequest.setStudentId(otherStudent.getId());
        otherRequest.setPlanId(otherPlan.getId());
        otherRequest.setStartDate(LocalDate.of(2026, 1, 15));
        createAndAuthenticateUser(otherStudio, UserRole.ADMIN);
        contractApplicationService.enroll(otherRequest);
        createAndAuthenticateUser(studio, UserRole.ADMIN);

        var otherInstallment = installmentRepository.findAll().get(0);

        mockMvc.perform(get("/finance/installments/{id}", otherInstallment.getId()))
                .andExpect(status().isNotFound());
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
        student.setBillingEnabled(true);
        return studentRepository.save(student);
    }

    private Plan createPlan(String name, BigDecimal price, Integer duration) {
        var plan = new Plan();
        plan.setStudio(studio);
        plan.setName(name);
        plan.setPrice(price);
        plan.setDuration(duration);
        plan.setVersion(1);
        plan.setActive(true);
        plan.setAutoRenew(true);
        return plan;
    }

    private void createAndAuthenticateUser(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail("installment_" + UUID.randomUUID() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
