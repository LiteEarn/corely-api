package br.com.corely.comercial.overdue;

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
class OverdueProcessingIntegrationTest {

    @Autowired
    private OverdueProcessingService overdueProcessingService;

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
        studio = studioRepository.save(createStudio("Overdue Studio"));
        authenticateAs(studio, UserRole.OWNER);

        student = studentRepository.save(createStudent(studio, "John Doe"));

        var plan = planRepository.save(createPlan("Premium Plan", BigDecimal.valueOf(299), 30));
        var validityDays = ruleDefinitionRepository.save(createRuleDef("VALIDITY_DAYS", ValueType.INTEGER));
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));

        var spRequest = new StudentPlanRequest();
        spRequest.setStudentId(student.getId());
        spRequest.setPlanId(plan.getId());
        spRequest.setStartDate(LocalDate.of(2026, 1, 15));
        studentPlanId = contractApplicationService.enroll(spRequest).getId();
    }

    private UUID createInvoice(LocalDate dueDate, InvoiceStatus status) {
        var request = new InvoiceRequest();
        request.setStudentPlanId(studentPlanId);
        request.setDueDate(dueDate);
        request.setReferenceMonth(dueDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        var response = invoiceService.create(request);
        if (status == InvoiceStatus.OVERDUE) {
            var invoice = invoiceRepository.findById(response.getId()).orElseThrow();
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
        }
        return response.getId();
    }

    @Test
    void process_shouldMarkOverdueInvoices() {
        createInvoice(LocalDate.of(2026, 1, 15), InvoiceStatus.PENDING);

        var result = overdueProcessingService.process(LocalDate.of(2026, 2, 1));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getOverdue()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);

        var invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        assertThat(invoices.get(0).getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
    }

    @Test
    void process_shouldNotMarkPaidInvoices() {
        var invoiceId = createInvoice(LocalDate.of(2026, 1, 15), InvoiceStatus.PENDING);
        var invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        var result = overdueProcessingService.process(LocalDate.of(2026, 2, 1));

        assertThat(result.getProcessed()).isEqualTo(0);
    }

    @Test
    void process_shouldNotMarkCancelledInvoices() {
        var invoiceId = createInvoice(LocalDate.of(2026, 1, 15), InvoiceStatus.PENDING);
        var invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);

        var result = overdueProcessingService.process(LocalDate.of(2026, 2, 1));

        assertThat(result.getProcessed()).isEqualTo(0);
    }

    @Test
    void process_shouldNotMarkAlreadyOverdueInvoices() {
        createInvoice(LocalDate.of(2026, 1, 15), InvoiceStatus.OVERDUE);

        var result = overdueProcessingService.process(LocalDate.of(2026, 2, 1));

        assertThat(result.getProcessed()).isEqualTo(0);
    }

    @Test
    void process_shouldNotMarkFutureInvoices() {
        createInvoice(LocalDate.of(2026, 3, 15), InvoiceStatus.PENDING);

        var result = overdueProcessingService.process(LocalDate.of(2026, 2, 1));

        assertThat(result.getProcessed()).isEqualTo(0);
    }

    @Test
    void process_shouldOnlyMarkOverdueWhenDueDateBeforeProcessingDate() {
        createInvoice(LocalDate.of(2026, 1, 15), InvoiceStatus.PENDING);

        var result = overdueProcessingService.process(LocalDate.of(2026, 1, 15));

        assertThat(result.getProcessed()).isEqualTo(0);
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
