package br.com.corely.comercial.contractreactivation;

import br.com.corely.comercial.contract.ContractApplicationService;
import br.com.corely.comercial.invoice.Invoice;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceService;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.comercial.invoice.dto.InvoiceRequest;
import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.*;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.comercial.studentplan.SuspensionReason;
import br.com.corely.comercial.studentplan.dto.StudentPlanRequest;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContractReactivationIntegrationTest {

    @Autowired
    private ContractReactivationService contractReactivationService;

    @Autowired
    private StudentPlanRepository studentPlanRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ContractApplicationService contractApplicationService;

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
    private UUID studentPlanId;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Reactivation Studio"));
        authenticateAs(studio, UserRole.OWNER);

        student = studentRepository.save(createStudent(studio, "John Reactivate"));

        var plan = planRepository.save(createPlan("Standard Plan", BigDecimal.valueOf(199), 30));
        var validityDays = ruleDefinitionRepository.save(createRuleDef("VALIDITY_DAYS", ValueType.INTEGER));
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        var spRequest = new StudentPlanRequest();
        spRequest.setStudentId(student.getId());
        spRequest.setPlanId(plan.getId());
        spRequest.setStartDate(LocalDate.of(2026, 6, 1));
        studentPlanId = contractApplicationService.enroll(spRequest).getId();
    }

    private void suspendPlan() {
        var studentPlan = studentPlanRepository.findById(studentPlanId).orElseThrow();
        studentPlan.setStatus(StudentPlanStatus.SUSPENDED);
        studentPlan.setBookingBlocked(true);
        studentPlan.setSuspensionReason(SuspensionReason.DELINQUENCY);
        studentPlanRepository.save(studentPlan);
    }

    private UUID createOverdueInvoice(LocalDate dueDate) {
        var request = new InvoiceRequest();
        request.setStudentPlanId(studentPlanId);
        request.setDueDate(dueDate);
        request.setReferenceMonth(dueDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        var response = invoiceService.create(request);
        var invoice = invoiceRepository.findById(response.getId()).orElseThrow();
        invoice.setStatus(InvoiceStatus.OVERDUE);
        invoiceRepository.save(invoice);
        return response.getId();
    }

    @Test
    void process_shouldReactivateSuspendedPlanWithoutOverdue() {
        suspendPlan();

        var result = contractReactivationService.process(LocalDate.of(2026, 7, 14));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getReactivated()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);

        var studentPlan = studentPlanRepository.findById(studentPlanId).orElseThrow();
        assertThat(studentPlan.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);
        assertThat(studentPlan.getBookingBlocked()).isFalse();
    }

    @Test
    void process_shouldSkipWhenOverdueInvoicesExist() {
        createOverdueInvoice(LocalDate.of(2026, 6, 15));
        suspendPlan();

        var result = contractReactivationService.process(LocalDate.of(2026, 7, 14));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getReactivated()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);

        var studentPlan = studentPlanRepository.findById(studentPlanId).orElseThrow();
        assertThat(studentPlan.getStatus()).isEqualTo(StudentPlanStatus.SUSPENDED);
        assertThat(studentPlan.getBookingBlocked()).isTrue();
    }

    @Test
    void process_shouldSkipWhenSuspensionReasonIsNotDelinquency() {
        var studentPlan = studentPlanRepository.findById(studentPlanId).orElseThrow();
        studentPlan.setStatus(StudentPlanStatus.SUSPENDED);
        studentPlan.setBookingBlocked(true);
        studentPlan.setSuspensionReason(SuspensionReason.MANUAL);
        studentPlanRepository.save(studentPlan);

        var result = contractReactivationService.process(LocalDate.of(2026, 7, 14));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getReactivated()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);

        var reloaded = studentPlanRepository.findById(studentPlanId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(StudentPlanStatus.SUSPENDED);
    }

    @Test
    void process_shouldNotReactivateWhenPlanIsActive() {
        var result = contractReactivationService.process(LocalDate.of(2026, 7, 14));

        assertThat(result.getProcessed()).isEqualTo(0);
        assertThat(result.getReactivated()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
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