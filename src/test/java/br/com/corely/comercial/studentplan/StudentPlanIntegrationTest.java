package br.com.corely.comercial.studentplan;

import br.com.corely.comercial.contractsnapshot.ContractSnapshotRepository;
import br.com.corely.comercial.contractsnapshot.ContractSnapshotService;
import br.com.corely.comercial.plan.Plan;
import br.com.corely.comercial.plan.PlanRepository;
import br.com.corely.comercial.planrule.PlanRule;
import br.com.corely.comercial.planrule.PlanRuleRepository;
import br.com.corely.comercial.ruledefinition.*;
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
class StudentPlanIntegrationTest {

    @Autowired
    private StudentPlanService studentPlanService;

    @Autowired
    private StudentPlanRepository studentPlanRepository;

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
    private ContractSnapshotService contractSnapshotService;

    @Autowired
    private ContractSnapshotRepository contractSnapshotRepository;

    private Studio studio;
    private Student student;
    private Plan plan;
    private RuleDefinition validityDays;

    @BeforeEach
    void setUp() {
        studio = studioRepository.save(createStudio("Student Plan Studio"));
        authenticateAs(studio, UserRole.OWNER);

        student = studentRepository.save(createStudent(studio, "Jane Doe"));
        plan = planRepository.save(createPlan("Premium Plan", BigDecimal.valueOf(299), 30));

        validityDays = ruleDefinitionRepository.save(createRuleDef("VALIDITY_DAYS", ValueType.INTEGER));
        planRuleRepository.save(createPlanRule(plan, validityDays, "30"));
    }

    @Test
    void create_shouldCreateSnapshotAndStudentPlan() {
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.now());

        var response = studentPlanService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(student.getId());
        assertThat(response.getContractSnapshotId()).isNotNull();
        assertThat(response.getSnapshotName()).isEqualTo("Premium Plan");
        assertThat(response.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);

        assertThat(contractSnapshotRepository.count()).isEqualTo(1);
    }

    @Test
    void create_shouldThrowException_whenDuplicateActive() {
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.now());
        studentPlanService.create(request);

        var duplicate = new StudentPlanRequest();
        duplicate.setStudentId(student.getId());
        duplicate.setPlanId(plan.getId());
        duplicate.setStartDate(LocalDate.now());

        assertThatThrownBy(() -> studentPlanService.create(duplicate))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Student already has an active plan.");
    }

    @Test
    void create_shouldAllowNewPlanAfterCancellation() {
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.now());
        var created = studentPlanService.create(request);

        studentPlanService.cancel(created.getId());

        var newRequest = new StudentPlanRequest();
        newRequest.setStudentId(student.getId());
        newRequest.setPlanId(plan.getId());
        newRequest.setStartDate(LocalDate.now());
        var response = studentPlanService.create(newRequest);

        assertThat(response.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);
    }

    @Test
    void cancel_shouldSetStatus() {
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.now());
        var created = studentPlanService.create(request);

        var response = studentPlanService.cancel(created.getId());

        assertThat(response.getStatus()).isEqualTo(StudentPlanStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrowException_whenAlreadyCancelled() {
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.now());
        var created = studentPlanService.create(request);
        studentPlanService.cancel(created.getId());

        assertThatThrownBy(() -> studentPlanService.cancel(created.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void suspend_shouldSetStatus() {
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.now());
        var created = studentPlanService.create(request);

        var response = studentPlanService.suspend(created.getId());

        assertThat(response.getStatus()).isEqualTo(StudentPlanStatus.SUSPENDED);
    }

    @Test
    void reactivate_shouldRestoreActive() {
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.now());
        var created = studentPlanService.create(request);
        studentPlanService.suspend(created.getId());

        var response = studentPlanService.reactivate(created.getId());

        assertThat(response.getStatus()).isEqualTo(StudentPlanStatus.ACTIVE);
    }

    @Test
    void reactivate_shouldThrowException_whenAlreadyActive() {
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.now());
        var created = studentPlanService.create(request);

        assertThatThrownBy(() -> studentPlanService.reactivate(created.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void findById_shouldReturnStudentPlan() {
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.now());
        var created = studentPlanService.create(request);

        var response = studentPlanService.findById(created.getId());

        assertThat(response.getId()).isEqualTo(created.getId());
        assertThat(response.getStudentName()).isEqualTo("Jane Doe");
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        assertThatThrownBy(() -> studentPlanService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("StudentPlan not found");
    }

    @Test
    void findAll_shouldReturnAll() {
        var request = new StudentPlanRequest();
        request.setStudentId(student.getId());
        request.setPlanId(plan.getId());
        request.setStartDate(LocalDate.now());
        studentPlanService.create(request);

        var all = studentPlanService.findAll();

        assertThat(all).hasSize(1);
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
