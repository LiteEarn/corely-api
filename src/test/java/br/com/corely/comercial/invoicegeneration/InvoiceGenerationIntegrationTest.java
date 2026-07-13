package br.com.corely.comercial.invoicegeneration;

import br.com.corely.comercial.billingschedule.BillingSchedule;
import br.com.corely.comercial.billingschedule.BillingScheduleRepository;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.*;
import br.com.corely.comercial.studentplan.StudentPlanService;
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
class InvoiceGenerationIntegrationTest {

    @Autowired
    private InvoiceGenerationService invoiceGenerationService;

    @Autowired
    private BillingScheduleRepository billingScheduleRepository;

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
    private UUID studentPlanId;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("InvoiceGen Studio"));
        authenticateAs(studio, UserRole.OWNER);

        student = studentRepository.save(createStudent(studio, "John Doe"));

        var plan = planRepository.save(createPlan("Premium Plan", BigDecimal.valueOf(299), 30));
        var validityDays = ruleDefinitionRepository.save(createRuleDef("VALIDITY_DAYS", ValueType.INTEGER));
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        var spRequest = new StudentPlanRequest();
        spRequest.setStudentId(student.getId());
        spRequest.setPlanId(plan.getId());
        spRequest.setStartDate(LocalDate.of(2026, 1, 15));
        studentPlanId = studentPlanService.create(spRequest).getId();
    }

    @Test
    void process_shouldGenerateInvoiceAndUpdateSchedule() {
        var result = invoiceGenerationService.process(LocalDate.of(2026, 1, 20));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getGenerated()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);

        var invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        assertThat(invoices).hasSize(1);
        var invoice = invoices.get(0);
        assertThat(invoice.getStudentPlan().getId()).isEqualTo(studentPlanId);
        assertThat(invoice.getReferenceMonth()).isEqualTo("2026-01");
        assertThat(invoice.getAmount()).isEqualByComparingTo(new BigDecimal("299"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(invoice.getIssueDate()).isEqualTo(LocalDate.of(2026, 1, 20));

        var schedule = billingScheduleRepository.findByStudentPlanId(studentPlanId).orElseThrow();
        assertThat(schedule.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 2, 15));
    }

    @Test
    void process_shouldNotGenerateDuplicateInvoice() {
        invoiceGenerationService.process(LocalDate.of(2026, 1, 20));
        invoiceGenerationService.process(LocalDate.of(2026, 2, 20));

        var invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        assertThat(invoices).hasSize(2);
        assertThat(invoices.get(0).getReferenceMonth()).isEqualTo("2026-02");
        assertThat(invoices.get(1).getReferenceMonth()).isEqualTo("2026-01");

        var result = invoiceGenerationService.process(LocalDate.of(2026, 2, 20));

        assertThat(result.getProcessed()).isEqualTo(0);
        assertThat(result.getGenerated()).isEqualTo(0);

        invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        assertThat(invoices).hasSize(2);
    }

    @Test
    void process_shouldGenerateNextMonthInvoiceAfterAdvancing() {
        invoiceGenerationService.process(LocalDate.of(2026, 1, 20));

        var result = invoiceGenerationService.process(LocalDate.of(2026, 2, 20));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getGenerated()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);

        var invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        assertThat(invoices).hasSize(2);

        var schedule = billingScheduleRepository.findByStudentPlanId(studentPlanId).orElseThrow();
        assertThat(schedule.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void process_shouldNotGenerateWhenProcessingDateBeforeNextBilling() {
        var result = invoiceGenerationService.process(LocalDate.of(2026, 1, 10));

        assertThat(result.getProcessed()).isEqualTo(0);
        assertThat(result.getGenerated()).isEqualTo(0);

        var invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        assertThat(invoices).isEmpty();
    }

    @Test
    void process_shouldGenerateForMultipleConsecutiveMonths() {
        invoiceGenerationService.process(LocalDate.of(2026, 1, 20));
        invoiceGenerationService.process(LocalDate.of(2026, 2, 20));
        invoiceGenerationService.process(LocalDate.of(2026, 3, 20));

        var invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        assertThat(invoices).hasSize(3);
        assertThat(invoices.get(0).getReferenceMonth()).isEqualTo("2026-03");
        assertThat(invoices.get(1).getReferenceMonth()).isEqualTo("2026-02");
        assertThat(invoices.get(2).getReferenceMonth()).isEqualTo("2026-01");

        var schedule = billingScheduleRepository.findByStudentPlanId(studentPlanId).orElseThrow();
        assertThat(schedule.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 4, 15));
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
