package br.com.corely.comercial.contractexpiration;

import br.com.corely.comercial.billingschedule.BillingSchedule;
import br.com.corely.comercial.billingschedule.BillingScheduleRepository;
import br.com.corely.comercial.contract.ContractApplicationService;
import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.*;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
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
class ContractExpirationIntegrationTest {

    @Autowired
    private ContractExpirationService contractExpirationService;

    @Autowired
    private StudentPlanRepository studentPlanRepository;

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

    @Autowired
    private BillingScheduleRepository billingScheduleRepository;

    private Studio studio;
    private Student student;
    private Student student2;
    private Plan planNoAutoRenew;
    private Plan planAutoRenew;
    private UUID studentPlanIdNoRenew;
    private UUID studentPlanIdAutoRenew;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Expiration Studio"));
        authenticateAs(studio, UserRole.OWNER);

        student = studentRepository.save(createStudent(studio, "John Expiration No Renew"));
        student2 = studentRepository.save(createStudent(studio, "Jane Expiration Auto Renew"));

        planNoAutoRenew = planRepository.save(createPlan("Fixed Plan", BigDecimal.valueOf(150), 30, false));
        planAutoRenew = planRepository.save(createPlan("Auto Renew Plan", BigDecimal.valueOf(200), 30, true));
        var validityDays = ruleDefinitionRepository.save(createRuleDef("VALIDITY_DAYS", ValueType.INTEGER));
        planRuleRepository.save(createPlanRule(planNoAutoRenew, validityDays, "30"));
        planRuleRepository.save(createPlanRule(planAutoRenew, validityDays, "30"));

        var spRequestNoRenew = new StudentPlanRequest();
        spRequestNoRenew.setStudentId(student.getId());
        spRequestNoRenew.setPlanId(planNoAutoRenew.getId());
        spRequestNoRenew.setStartDate(LocalDate.of(2026, 6, 1));
        var responseNoRenew = contractApplicationService.enroll(spRequestNoRenew);
        studentPlanIdNoRenew = responseNoRenew.getId();

        var spRequestAutoRenew = new StudentPlanRequest();
        spRequestAutoRenew.setStudentId(student2.getId());
        spRequestAutoRenew.setPlanId(planAutoRenew.getId());
        spRequestAutoRenew.setStartDate(LocalDate.of(2026, 6, 1));
        var responseAutoRenew = contractApplicationService.enroll(spRequestAutoRenew);
        studentPlanIdAutoRenew = responseAutoRenew.getId();
    }

    private void expireContract(UUID studentPlanId) {
        var studentPlan = studentPlanRepository.findById(studentPlanId).orElseThrow();
        studentPlan.setEndDate(LocalDate.of(2026, 6, 30));
        studentPlanRepository.save(studentPlan);
    }

    @Test
    void process_shouldFinishContractWhenAutoRenewIsFalse() {
        expireContract(studentPlanIdNoRenew);

        var result = contractExpirationService.process(LocalDate.of(2026, 7, 14));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getFinished()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);

        var studentPlan = studentPlanRepository.findById(studentPlanIdNoRenew).orElseThrow();
        assertThat(studentPlan.getStatus()).isEqualTo(StudentPlanStatus.FINISHED);
        assertThat(studentPlan.getBookingBlocked()).isFalse();
    }

    @Test
    void process_shouldSkipContractWhenAutoRenewIsTrue() {
        expireContract(studentPlanIdAutoRenew);

        var result = contractExpirationService.process(LocalDate.of(2026, 7, 14));

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getFinished()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).isEqualTo(0);

        var studentPlan = studentPlanRepository.findById(studentPlanIdAutoRenew).orElseThrow();
        assertThat(studentPlan.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);
    }

    @Test
    void process_shouldDeactivateBillingSchedule() {
        expireContract(studentPlanIdNoRenew);

        var scheduleBefore = billingScheduleRepository.findByStudentPlanId(studentPlanIdNoRenew);
        assertThat(scheduleBefore).isPresent();
        assertThat(scheduleBefore.get().getActive()).isTrue();

        contractExpirationService.process(LocalDate.of(2026, 7, 14));

        var scheduleAfter = billingScheduleRepository.findByStudentPlanId(studentPlanIdNoRenew);
        assertThat(scheduleAfter).isPresent();
        assertThat(scheduleAfter.get().getActive()).isFalse();
    }

    @Test
    void process_shouldRemoveBookingBlocked() {
        expireContract(studentPlanIdNoRenew);

        var studentPlan = studentPlanRepository.findById(studentPlanIdNoRenew).orElseThrow();
        studentPlan.setBookingBlocked(true);
        studentPlanRepository.save(studentPlan);

        contractExpirationService.process(LocalDate.of(2026, 7, 14));

        var updated = studentPlanRepository.findById(studentPlanIdNoRenew).orElseThrow();
        assertThat(updated.getBookingBlocked()).isFalse();
    }

    @Test
    void process_shouldNotProcessNonExpiredContracts() {
        var result = contractExpirationService.process(LocalDate.of(2026, 6, 15));

        assertThat(result.getProcessed()).isEqualTo(0);
        assertThat(result.getFinished()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
    }

    @Test
    void process_shouldNotFinishCancelledContract() {
        expireContract(studentPlanIdNoRenew);

        var studentPlan = studentPlanRepository.findById(studentPlanIdNoRenew).orElseThrow();
        studentPlan.setStatus(StudentPlanStatus.CANCELLED);
        studentPlanRepository.save(studentPlan);

        var result = contractExpirationService.process(LocalDate.of(2026, 7, 14));

        assertThat(result.getProcessed()).isEqualTo(0);
    }

    @Test
    void process_shouldNotFinishSuspendedContract() {
        expireContract(studentPlanIdNoRenew);

        var studentPlan = studentPlanRepository.findById(studentPlanIdNoRenew).orElseThrow();
        studentPlan.setStatus(StudentPlanStatus.SUSPENDED);
        studentPlanRepository.save(studentPlan);

        var result = contractExpirationService.process(LocalDate.of(2026, 7, 14));

        assertThat(result.getProcessed()).isEqualTo(0);
    }

    @Test
    void process_shouldNotFinishAlreadyFinishedContract() {
        expireContract(studentPlanIdNoRenew);

        var studentPlan = studentPlanRepository.findById(studentPlanIdNoRenew).orElseThrow();
        studentPlan.setStatus(StudentPlanStatus.FINISHED);
        studentPlanRepository.save(studentPlan);

        var result = contractExpirationService.process(LocalDate.of(2026, 7, 14));

        assertThat(result.getProcessed()).isEqualTo(0);
    }

    @Test
    void process_shouldHandleBothExpiredContracts() {
        expireContract(studentPlanIdNoRenew);
        expireContract(studentPlanIdAutoRenew);

        var result = contractExpirationService.process(LocalDate.of(2026, 7, 14));

        assertThat(result.getProcessed()).isEqualTo(2);

        var finishedPlan = studentPlanRepository.findById(studentPlanIdNoRenew).orElseThrow();
        assertThat(finishedPlan.getStatus()).isEqualTo(StudentPlanStatus.FINISHED);

        var autoRenewPlan = studentPlanRepository.findById(studentPlanIdAutoRenew).orElseThrow();
        assertThat(autoRenewPlan.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);
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

    private Plan createPlan(String name, BigDecimal price, Integer duration, Boolean autoRenew) {
        var plan = new Plan();
        plan.setStudio(studio);
        plan.setName(name);
        plan.setPrice(price);
        plan.setDuration(duration);
        plan.setVersion(1);
        plan.setActive(true);
        plan.setAutoRenew(autoRenew);
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
