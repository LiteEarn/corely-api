package br.com.corely.finance.refund;

import br.com.corely.finance.installment.InstallmentStatus;
import br.com.corely.finance.installment.ReceivableInstallment;
import br.com.corely.finance.installment.ReceivableInstallmentRepository;
import br.com.corely.finance.movement.MovementType;
import br.com.corely.finance.movement.ReceivableMovementService;
import br.com.corely.finance.payment.Payment;
import br.com.corely.finance.payment.PaymentMethod;
import br.com.corely.finance.payment.PaymentRepository;
import br.com.corely.finance.receivable.Receivable;
import br.com.corely.finance.receivable.ReceivableRepository;
import br.com.corely.finance.receivable.ReceivableStatus;
import br.com.corely.finance.refund.dto.RefundRequest;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
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
import java.time.LocalDateTime;
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
 * Testes unitários do serviço de estorno de pagamentos (EPIC-03-S10).
 */
@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReceivableRepository receivableRepository;

    @Mock
    private ReceivableInstallmentRepository installmentRepository;

    @Mock
    private ReceivableMovementService movementService;

    @Mock
    private TenantContext tenantContext;

    private RefundService service;

    private UUID studioId;
    private Studio studio;
    private Student student;

    @BeforeEach
    void setUp() {
        service = new RefundService(paymentRepository, receivableRepository, installmentRepository,
                movementService, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);
        student = new Student();
        student.setId(UUID.randomUUID());
        student.setFullName("Refund Student");
    }

    private Receivable createReceivable(ReceivableStatus status) {
        var receivable = new Receivable();
        receivable.setId(UUID.randomUUID());
        receivable.setStudio(studio);
        receivable.setStudent(student);
        receivable.setAmount(BigDecimal.valueOf(150));
        receivable.setStatus(status);
        return receivable;
    }

    private Payment createPayment(Receivable receivable, ReceivableInstallment installment) {
        var payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setStudio(studio);
        payment.setReceivable(receivable);
        payment.setInstallment(installment);
        payment.setPaymentDate(LocalDate.now());
        payment.setAmount(BigDecimal.valueOf(150));
        payment.setPaymentMethod(PaymentMethod.CASH);
        return payment;
    }

    @Test
    void create_shouldRefundPaymentAndReopenReceivable() {
        var receivable = createReceivable(ReceivableStatus.PAID);
        var payment = createPayment(receivable, null);

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(installmentRepository.findByReceivableIdAndStatus(receivable.getId(), InstallmentStatus.PAID))
                .thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        var request = new RefundRequest();
        request.setPaymentId(payment.getId());
        request.setReason("Cliente desistiu");

        var response = service.create(request);

        assertThat(response.getId()).isEqualTo(payment.getId());
        assertThat(response.getReason()).isEqualTo("Cliente desistiu");
        assertThat(response.getRefundedAt()).isNotNull();
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.OPEN);
        verify(receivableRepository).save(receivable);
        verify(movementService).record(eq(receivable.getId()), eq(studioId), eq(MovementType.REFUND),
                eq(BigDecimal.valueOf(150)), eq("Cliente desistiu"));
    }

    @Test
    void create_shouldRefundInstallmentAndReopenIt() {
        var receivable = createReceivable(ReceivableStatus.PAID);
        var installment = new ReceivableInstallment();
        installment.setId(UUID.randomUUID());
        installment.setReceivable(receivable);
        installment.setStatus(InstallmentStatus.PAID);
        var payment = createPayment(receivable, installment);

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        var request = new RefundRequest();
        request.setPaymentId(payment.getId());

        service.create(request);

        assertThat(installment.getStatus()).isEqualTo(InstallmentStatus.OPEN);
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.OPEN);
        verify(installmentRepository).save(installment);
        verify(receivableRepository).save(receivable);
    }

    @Test
    void create_shouldReturn404WhenPaymentNotFound() {
        var paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        var request = new RefundRequest();
        request.setPaymentId(paymentId);

        Throwable thrown = catchThrowable(() -> service.create(request));

        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        verify(movementService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void create_shouldReturn409WhenAlreadyRefunded() {
        var receivable = createReceivable(ReceivableStatus.OPEN);
        var payment = createPayment(receivable, null);
        payment.setRefundedAt(LocalDateTime.now());

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        var request = new RefundRequest();
        request.setPaymentId(payment.getId());

        Throwable thrown = catchThrowable(() -> service.create(request));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        assertThat(thrown.getMessage()).isEqualTo("Payment already refunded");
    }

    @Test
    void findAll_shouldReturnOnlyRefundedPayments() {
        var receivable = createReceivable(ReceivableStatus.OPEN);
        var payment = createPayment(receivable, null);
        payment.setRefundedAt(LocalDateTime.now());
        var pageable = PageRequest.of(0, 10);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(paymentRepository.findRefundedByStudioId(studioId, pageable))
                .thenReturn(new PageImpl<>(List.of(payment)));

        Page<br.com.corely.finance.refund.dto.RefundResponse> result = service.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStudentName()).isEqualTo("Refund Student");
    }
}
