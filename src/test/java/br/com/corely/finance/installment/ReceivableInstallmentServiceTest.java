package br.com.corely.finance.installment;

import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.finance.installment.dto.InstallmentResponse;
import br.com.corely.finance.receivable.Receivable;
import br.com.corely.finance.receivable.ReceivableRepository;
import br.com.corely.shared.tenant.TenantContext;
import br.com.corely.student.Student;
import br.com.corely.studio.Studio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do serviço de parcelas (EPIC-03-S02).
 */
@ExtendWith(MockitoExtension.class)
class ReceivableInstallmentServiceTest {

    @Mock
    private ReceivableInstallmentRepository installmentRepository;

    @Mock
    private ReceivableRepository receivableRepository;

    @Mock
    private TenantContext tenantContext;

    private ReceivableInstallmentService service;

    private Studio studio;
    private Student student;
    private StudentPlan studentPlan;

    @BeforeEach
    void setUp() {
        service = new ReceivableInstallmentService(installmentRepository, receivableRepository, tenantContext);

        studio = new Studio();
        studio.setId(UUID.randomUUID());
        studio.setName("Test Studio");

        student = new Student();
        student.setId(UUID.randomUUID());
        student.setStudio(studio);
        student.setFullName("Installment Student");

        studentPlan = new StudentPlan();
        studentPlan.setId(UUID.randomUUID());
        studentPlan.setStudio(studio);
        studentPlan.setStudent(student);
        studentPlan.setStartDate(LocalDate.of(2026, 1, 15));
    }

    @Test
    void generate_shouldCreateInstallmentsWithMonthlyDueDates() {
        var receivable = new Receivable();
        receivable.setId(UUID.randomUUID());
        receivable.setStudio(studio);
        receivable.setAmount(BigDecimal.valueOf(300));

        when(installmentRepository.save(any(ReceivableInstallment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = service.generate(studentPlan, receivable, 3, LocalDate.of(2026, 1, 15));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getInstallmentNumber()).isEqualTo(1);
        assertThat(result.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(result.get(1).getDueDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(result.get(2).getDueDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(result.get(0).getStatus()).isEqualTo(InstallmentStatus.OPEN);
    }

    @Test
    void generate_shouldSplitAmountEvenlyWithLastInstallmentAdjustment() {
        var receivable = new Receivable();
        receivable.setId(UUID.randomUUID());
        receivable.setStudio(studio);
        receivable.setAmount(BigDecimal.valueOf(100));

        when(installmentRepository.save(any(ReceivableInstallment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = service.generate(studentPlan, receivable, 3, LocalDate.of(2026, 1, 15));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("33.33");
        assertThat(result.get(1).getAmount()).isEqualByComparingTo("33.33");
        assertThat(result.get(2).getAmount()).isEqualByComparingTo("33.34");

        BigDecimal sum = result.stream()
                .map(ReceivableInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("100.00");
    }

    @Test
    void generate_withZeroOrNegativeCount_shouldReturnEmpty() {
        var receivable = new Receivable();
        receivable.setAmount(BigDecimal.TEN);

        assertThat(service.generate(studentPlan, receivable, 0, LocalDate.now())).isEmpty();
        assertThat(service.generate(studentPlan, receivable, -1, LocalDate.now())).isEmpty();
    }

    @Test
    void generateForStudentPlan_shouldCreateMasterReceivableAndInstallments() {
        var snapshot = new ContractSnapshot();
        snapshot.setPlanName("Premium");
        snapshot.setPlanPrice(BigDecimal.valueOf(299));
        snapshot.setPlanDuration(60); // 2 meses
        studentPlan.setContractSnapshot(snapshot);

        when(receivableRepository.save(any(Receivable.class))).thenAnswer(inv -> inv.getArgument(0));
        when(installmentRepository.save(any(ReceivableInstallment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = service.generateForStudentPlan(studentPlan);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStudentPlan().getId()).isEqualTo(studentPlan.getId());
        assertThat(result.get(0).getReceivable().getAmount()).isEqualByComparingTo("598.00");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("299.00");
        assertThat(result.get(1).getDueDate()).isEqualTo(LocalDate.of(2026, 2, 15));
    }

    @Test
    void findById_shouldReturnInstallment() {
        var installment = buildInstallment(1);
        when(installmentRepository.findById(installment.getId())).thenReturn(Optional.of(installment));

        var response = service.findById(installment.getId());

        assertThat(response).isNotNull();
        assertThat(response.getStudentName()).isEqualTo("Installment Student");
        assertThat(response.getInstallmentNumber()).isEqualTo(1);
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        when(installmentRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(br.com.corely.shared.exception.ResourceNotFoundException.class);
    }

    @Test
    void findAll_shouldDelegateFilters() {
        UUID studioId = studio.getId();
        UUID studentPlanId = studentPlan.getId();
        var installment = buildInstallment(2);

        var page = new PageImpl<>(List.of(installment), PageRequest.of(0, 10), 1);
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(installmentRepository.findByFilters(eq(studioId), eq(InstallmentStatus.OPEN), eq(studentPlanId),
                any(), any(), any(PageRequest.class))).thenReturn(page);

        Page<InstallmentResponse> result = service.findAll(
                InstallmentStatus.OPEN, studentPlanId, null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus().name()).isEqualTo("OPEN");
    }

    @Test
    void updateDueDate_shouldUpdateOpenInstallment() {
        var installment = buildInstallment(1);
        when(installmentRepository.findById(installment.getId())).thenReturn(Optional.of(installment));
        when(installmentRepository.save(any(ReceivableInstallment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var response = service.updateDueDate(installment.getId(), LocalDate.of(2026, 3, 10));

        assertThat(response).isNotNull();
        assertThat(response.getDueDate()).isEqualTo(LocalDate.of(2026, 3, 10));
    }

    @Test
    void updateDueDate_shouldRejectPaidInstallment() {
        var installment = buildInstallment(1);
        installment.setStatus(InstallmentStatus.PAID);
        when(installmentRepository.findById(installment.getId())).thenReturn(Optional.of(installment));

        assertThatThrownBy(() -> service.updateDueDate(installment.getId(), LocalDate.of(2026, 3, 10)))
                .isInstanceOf(br.com.corely.shared.exception.BusinessException.class);
    }

    @Test
    void updateDueDate_shouldRejectCancelledInstallment() {
        var installment = buildInstallment(1);
        installment.setStatus(InstallmentStatus.CANCELLED);
        when(installmentRepository.findById(installment.getId())).thenReturn(Optional.of(installment));

        assertThatThrownBy(() -> service.updateDueDate(installment.getId(), LocalDate.of(2026, 3, 10)))
                .isInstanceOf(br.com.corely.shared.exception.BusinessException.class);
    }

    @Test
    void updateDueDate_shouldThrowWhenNotFound() {
        when(installmentRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateDueDate(UUID.randomUUID(), LocalDate.of(2026, 3, 10)))
                .isInstanceOf(br.com.corely.shared.exception.ResourceNotFoundException.class);
    }

    private ReceivableInstallment buildInstallment(int number) {
        var installment = new ReceivableInstallment();
        installment.setId(UUID.randomUUID());
        installment.setStudio(studio);
        installment.setReceivable(new Receivable());
        installment.setStudentPlan(studentPlan);
        installment.setInstallmentNumber(number);
        installment.setAmount(BigDecimal.valueOf(100));
        installment.setDueDate(LocalDate.of(2026, 1, 15));
        installment.setStatus(InstallmentStatus.OPEN);
        return installment;
    }
}
