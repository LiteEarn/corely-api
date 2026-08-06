package br.com.corely.finance.payment;

import br.com.corely.finance.installment.InstallmentStatus;
import br.com.corely.finance.installment.ReceivableInstallment;
import br.com.corely.finance.installment.ReceivableInstallmentRepository;
import br.com.corely.finance.movement.MovementType;
import br.com.corely.finance.movement.ReceivableMovementService;
import br.com.corely.finance.payment.dto.PaymentMethodDto;
import br.com.corely.finance.payment.dto.PaymentRequest;
import br.com.corely.finance.payment.dto.PaymentResponse;
import br.com.corely.finance.receivable.Receivable;
import br.com.corely.finance.receivable.ReceivableRepository;
import br.com.corely.finance.receivable.ReceivableStatus;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.shared.tenant.TenantContext;
import br.com.corely.student.Student;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do serviço de baixa manual de pagamento (EPIC-03-S06).
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReceivableRepository receivableRepository;

    @Mock
    private ReceivableInstallmentRepository installmentRepository;

    @Mock
    private ReceivableMovementService movementService;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private TenantContext tenantContext;

    private PaymentService service;

    private UUID studioId;
    private Studio studio;
    private Student student;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, receivableRepository, installmentRepository,
                movementService, studioRepository, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);

        student = new Student();
        student.setId(UUID.randomUUID());
        student.setFullName("Aluno Teste");
    }

    private Receivable openReceivable(BigDecimal amount) {
        var receivable = new Receivable();
        receivable.setId(UUID.randomUUID());
        receivable.setStudent(student);
        receivable.setAmount(amount);
        receivable.setDueDate(LocalDate.now().plusDays(30));
        receivable.setStatus(ReceivableStatus.OPEN);
        return receivable;
    }

    private PaymentRequest request(UUID receivableId, BigDecimal amount) {
        var request = new PaymentRequest();
        request.setReceivableId(receivableId);
        request.setPaymentDate(LocalDate.now());
        request.setAmount(amount);
        request.setPaymentMethod(PaymentMethodDto.PIX);
        return request;
    }

    @Test
    void create_shouldRegisterPaymentAndSettleReceivable() {
        var receivable = openReceivable(BigDecimal.valueOf(199.90));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(request(receivable.getId(), BigDecimal.valueOf(199.90)));

        assertThat(response.getReceivableId()).isEqualTo(receivable.getId());
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethodDto.PIX);
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.PAID);
        verify(receivableRepository).save(receivable);
        verify(movementService).record(eq(receivable.getId()), eq(studioId), eq(MovementType.PAYMENT),
                eq(BigDecimal.valueOf(199.90)), any(String.class));
    }

    @Test
    void create_shouldThrowWhenReceivableNotFound() {
        UUID receivableId = UUID.randomUUID();
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivableId)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> service.create(request(receivableId, BigDecimal.valueOf(100))));

        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_shouldThrowWhenReceivableNotOpen() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        receivable.setStatus(ReceivableStatus.PAID);
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));

        Throwable thrown = catchThrowable(() -> service.create(request(receivable.getId(), BigDecimal.valueOf(100))));

        assertThat(thrown).isInstanceOf(BusinessException.class);
    }

    @Test
    void create_shouldThrowWhenReceivableAlreadyPaid() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(true);

        Throwable thrown = catchThrowable(() -> service.create(request(receivable.getId(), BigDecimal.valueOf(100))));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
    }

    @Test
    void create_shouldThrowWhenAmountMismatch() {
        var receivable = openReceivable(BigDecimal.valueOf(199.90));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);

        Throwable thrown = catchThrowable(() -> service.create(request(receivable.getId(), BigDecimal.valueOf(100))));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
    }

    @Test
    void create_shouldSettleInstallmentAndReceivableWhenLastOpen() {
        var receivable = openReceivable(BigDecimal.valueOf(300));
        var installment = new ReceivableInstallment();
        installment.setId(UUID.randomUUID());
        installment.setReceivable(receivable);
        installment.setAmount(BigDecimal.valueOf(100));
        installment.setStatus(InstallmentStatus.OPEN);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(installmentRepository.findById(installment.getId())).thenReturn(Optional.of(installment));
        when(installmentRepository.countByReceivableIdAndStatus(receivable.getId(), InstallmentStatus.OPEN))
                .thenReturn(0L);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = request(receivable.getId(), BigDecimal.valueOf(100));
        request.setInstallmentId(installment.getId());

        var response = service.create(request);

        assertThat(response.getInstallmentId()).isEqualTo(installment.getId());
        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.PAID);
    }

    @Test
    void create_shouldSettleInstallmentButKeepReceivableOpenWhenOthersRemain() {
        var receivable = openReceivable(BigDecimal.valueOf(300));
        var installment = new ReceivableInstallment();
        installment.setId(UUID.randomUUID());
        installment.setReceivable(receivable);
        installment.setAmount(BigDecimal.valueOf(100));
        installment.setStatus(InstallmentStatus.OPEN);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(installmentRepository.findById(installment.getId())).thenReturn(Optional.of(installment));
        when(installmentRepository.countByReceivableIdAndStatus(receivable.getId(), InstallmentStatus.OPEN))
                .thenReturn(1L);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = request(receivable.getId(), BigDecimal.valueOf(100));
        request.setInstallmentId(installment.getId());

        var response = service.create(request);

        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.OPEN);
        verify(receivableRepository, never()).save(receivable);
    }

    @Test
    void create_shouldThrowWhenInstallmentBelongsToOtherReceivable() {
        var receivable = openReceivable(BigDecimal.valueOf(300));
        var otherReceivable = openReceivable(BigDecimal.valueOf(200));
        var installment = new ReceivableInstallment();
        installment.setId(UUID.randomUUID());
        installment.setReceivable(otherReceivable);
        installment.setAmount(BigDecimal.valueOf(100));
        installment.setStatus(InstallmentStatus.OPEN);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(installmentRepository.findById(installment.getId())).thenReturn(Optional.of(installment));

        var request = request(receivable.getId(), BigDecimal.valueOf(100));
        request.setInstallmentId(installment.getId());

        Throwable thrown = catchThrowable(() -> service.create(request));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        verify(paymentRepository, never()).saveAndFlush(any(Payment.class));
    }

    @Test
    void findAll_shouldReturnPaymentsOfStudio() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        var payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setReceivable(receivable);
        payment.setPaymentDate(LocalDate.now());
        payment.setAmount(BigDecimal.valueOf(100));
        payment.setPaymentMethod(PaymentMethod.PIX);

        var page = new PageImpl<>(List.of(payment), PageRequest.of(0, 10), 1);
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(paymentRepository.findByStudioId(eq(studioId), any(PageRequest.class))).thenReturn(page);

        Page<PaymentResponse> result = service.findAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReceivableId()).isEqualTo(receivable.getId());
        assertThat(result.getContent().get(0).getStudentName()).isEqualTo("Aluno Teste");
    }

    @Test
    void create_shouldSettleReceivableWhenOnlyCancelledInstallmentsRemain() {
        var receivable = openReceivable(BigDecimal.valueOf(300));
        var installment = new ReceivableInstallment();
        installment.setId(UUID.randomUUID());
        installment.setReceivable(receivable);
        installment.setAmount(BigDecimal.valueOf(100));
        installment.setStatus(InstallmentStatus.OPEN);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(installmentRepository.findById(installment.getId())).thenReturn(Optional.of(installment));
        when(installmentRepository.countByReceivableIdAndStatus(receivable.getId(), InstallmentStatus.OPEN))
                .thenReturn(0L);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = request(receivable.getId(), BigDecimal.valueOf(100));
        request.setInstallmentId(installment.getId());

        var response = service.create(request);

        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.PAID);
    }

    @Test
    void create_shouldSettleAllOpenInstallmentsWhenSettlingReceivableDirectly() {
        var receivable = openReceivable(BigDecimal.valueOf(300));
        var open1 = new ReceivableInstallment();
        open1.setId(UUID.randomUUID());
        open1.setReceivable(receivable);
        open1.setAmount(BigDecimal.valueOf(100));
        open1.setStatus(InstallmentStatus.OPEN);
        var open2 = new ReceivableInstallment();
        open2.setId(UUID.randomUUID());
        open2.setReceivable(receivable);
        open2.setAmount(BigDecimal.valueOf(200));
        open2.setStatus(InstallmentStatus.OPEN);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(installmentRepository.findByReceivableIdAndStatus(receivable.getId(), InstallmentStatus.OPEN))
                .thenReturn(List.of(open1, open2));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(request(receivable.getId(), BigDecimal.valueOf(300)));

        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.PAID);
        assertThat(open1.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(open2.getStatus()).isEqualTo(InstallmentStatus.PAID);
    }
}