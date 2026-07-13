package br.com.corely.comercial.invoice;

import br.com.corely.comercial.invoice.dto.InvoiceRequest;
import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.*;
import br.com.corely.comercial.studentplan.StudentPlanService;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.comercial.studentplan.dto.StudentPlanRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InvoiceIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private StudentPlanService studentPlanService;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanRuleRepository planRuleRepository;

    @Autowired
    private RuleDefinitionRepository ruleDefinitionRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;
    private Student student;
    private Plan plan;
    private UUID studentPlanId;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Invoice Studio"));
        authenticateAs(studio, UserRole.OWNER);

        student = studentRepository.save(createStudent(studio, "John Doe"));

        plan = planRepository.save(createPlan("Premium Plan", BigDecimal.valueOf(299), 30));
        var validityDays = ruleDefinitionRepository.save(createRuleDef("VALIDITY_DAYS", ValueType.INTEGER));
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        var spRequest = new StudentPlanRequest();
        spRequest.setStudentId(student.getId());
        spRequest.setPlanId(plan.getId());
        spRequest.setStartDate(LocalDate.now());
        studentPlanId = studentPlanService.create(spRequest).getId();
    }

    @Test
    void create_shouldCreateInvoiceFromSnapshotPrice() {
        var request = new InvoiceRequest();
        request.setStudentPlanId(studentPlanId);
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setReferenceMonth("2026-08");

        var response = invoiceService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStudentPlanId()).isEqualTo(studentPlanId);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("299"));
        assertThat(response.getPlanName()).isEqualTo("Premium Plan");
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(response.getReferenceMonth()).isEqualTo("2026-08");
    }

    @Test
    void create_shouldThrowException_whenDuplicateMonth() {
        var request = new InvoiceRequest();
        request.setStudentPlanId(studentPlanId);
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setReferenceMonth("2026-08");
        invoiceService.create(request);

        assertThatThrownBy(() -> invoiceService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invoice already exists for this month and student plan.");
    }

    @Test
    void create_shouldThrowException_whenStudentPlanNotActive() {
        var sp = studentPlanService.findById(studentPlanId);
        studentPlanService.cancel(sp.getId());

        var request = new InvoiceRequest();
        request.setStudentPlanId(studentPlanId);
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setReferenceMonth("2026-08");

        assertThatThrownBy(() -> invoiceService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot create invoice for a non-active student plan.");
    }

    @Test
    void create_shouldThrowException_whenStudentPlanNotFound() {
        var request = new InvoiceRequest();
        request.setStudentPlanId(UUID.randomUUID());
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setReferenceMonth("2026-08");

        assertThatThrownBy(() -> invoiceService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("StudentPlan not found");
    }

    @Test
    void cancel_shouldChangeStatus() {
        var createReq = new InvoiceRequest();
        createReq.setStudentPlanId(studentPlanId);
        createReq.setDueDate(LocalDate.of(2026, 8, 15));
        createReq.setReferenceMonth("2026-08");
        var created = invoiceService.create(createReq);

        var response = invoiceService.cancel(created.getId());

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrowException_whenAlreadyCancelled() {
        var createReq = new InvoiceRequest();
        createReq.setStudentPlanId(studentPlanId);
        createReq.setDueDate(LocalDate.of(2026, 8, 15));
        createReq.setReferenceMonth("2026-08");
        var created = invoiceService.create(createReq);
        invoiceService.cancel(created.getId());

        assertThatThrownBy(() -> invoiceService.cancel(created.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Only PENDING invoices can be cancelled.");
    }

    @Test
    void findById_shouldReturnInvoice() {
        var createReq = new InvoiceRequest();
        createReq.setStudentPlanId(studentPlanId);
        createReq.setDueDate(LocalDate.of(2026, 8, 15));
        createReq.setReferenceMonth("2026-08");
        var created = invoiceService.create(createReq);

        var response = invoiceService.findById(created.getId());

        assertThat(response.getId()).isEqualTo(created.getId());
        assertThat(response.getStudentName()).isEqualTo("John Doe");
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> invoiceService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invoice not found");
    }

    @Test
    void findAll_shouldReturnAllInvoices() {
        var req = new InvoiceRequest();
        req.setStudentPlanId(studentPlanId);
        req.setDueDate(LocalDate.of(2026, 8, 15));
        req.setReferenceMonth("2026-08");
        invoiceService.create(req);

        var all = invoiceService.findAll();

        assertThat(all).hasSize(1);
    }

    @Test
    void amountShouldComeFromSnapshotNotPlan() {
        var req = new InvoiceRequest();
        req.setStudentPlanId(studentPlanId);
        req.setDueDate(LocalDate.of(2026, 8, 15));
        req.setReferenceMonth("2026-08");
        var response = invoiceService.create(req);

        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("299"));
    }

    private void authenticateAs(Studio studio, UserRole role) {
        var user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "_" + studio.getId() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Studio createStudio(String name) {
        var studio = new Studio();
        studio.setName(name);
        studio.setActive(true);
        return studio;
    }

    private Student createStudent(Studio studio, String name) {
        var student = new Student();
        student.setStudio(studio);
        student.setFullName(name);
        student.setActive(true);
        return student;
    }

    private Plan createPlan(String name, BigDecimal price, Integer duration) {
        var plan = new Plan();
        plan.setStudio(studio);
        plan.setName(name);
        plan.setPrice(price);
        plan.setDuration(duration);
        plan.setVersion(1);
        plan.setActive(true);
        return plan;
    }

    private RuleDefinition createRuleDef(String code, ValueType valueType) {
        var rule = new RuleDefinition();
        rule.setCode(code);
        rule.setName(code);
        rule.setValueType(valueType);
        rule.setCategory(Category.GENERAL);
        rule.setActive(true);
        return rule;
    }

    private PlanRule createPlanRule(Plan plan, RuleDefinition ruleDef, String value) {
        var pr = new PlanRule();
        pr.setStudio(studio);
        pr.setPlan(plan);
        pr.setRuleDefinition(ruleDef);
        pr.setValue(value);
        return pr;
    }
}
