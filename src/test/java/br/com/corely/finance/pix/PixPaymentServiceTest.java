package br.com.corely.finance.pix;

import br.com.corely.finance.payment.PaymentRepository;
import br.com.corely.finance.payment.PaymentService;
import br.com.corely.finance.payment.dto.PaymentMethodDto;
import br.com.corely.finance.payment.dto.PaymentRequest;
import br.com.corely.finance.pix.dto.PixPaymentRequest;
import br.com.corely.finance.pix.dto.PixPaymentResponse;
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
import org.mockito.ArgumentCaptor;
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
 * Testes unitários do serviço de cobranças Pix (EPIC-03-S07).
 */
@ExtendWith(MockitoExtension.class)
class PixPaymentServiceTest {

    @Mock
    private PixPaymentRepository pixPaymentRepository;

    @Mock
    private ReceivableRepository receivableRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private TenantContext tenantContext;

    private PixPaymentService service;

    private UUID studioId;
    private Studio studio;
    private Student student;

    @BeforeEach
    void setUp() {
        service = new PixPaymentService(pixPaymentRepository, receivableRepository, paymentRepository,
                paymentService, studioRepository, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);

        student = new Student();
        student.setId(UUID.randomUUID());
        student.setFullName("Aluno Pix");
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

    private PixPaymentRequest request(UUID receivableId) {
        var request = new PixPaymentRequest();
        request.setReceivableId(receivableId);
        return request;
    }

    @Test
    void create_shouldGeneratePixCharge() {
        var receivable = openReceivable(BigDecimal.valueOf(199.90));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(pixPaymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(pixPaymentRepository.saveAndFlush(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        PixPaymentResponse response = service.create(request(receivable.getId()));

        assertThat(response.getReceivableId()).isEqualTo(receivable.getId());
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(199.90));
        assertThat(response.getStatus()).isEqualTo(PixPaymentStatus.PENDING);
        assertThat(response.getTxid()).hasSize(32);
        assertThat(response.getCopyPaste()).contains(response.getTxid());
        assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void create_shouldThrowWhenReceivableNotFound() {
        UUID receivableId = UUID.randomUUID();
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivableId)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> service.create(request(receivableId)));

        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_shouldThrowWhenReceivableNotOpen() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        receivable.setStatus(ReceivableStatus.PAID);
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));

        Throwable thrown = catchThrowable(() -> service.create(request(receivable.getId())));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        verify(pixPaymentRepository, never()).saveAndFlush(any(PixPayment.class));
    }

    @Test
    void create_shouldThrowWhenReceivableHasPayment() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(true);

        Throwable thrown = catchThrowable(() -> service.create(request(receivable.getId())));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        verify(pixPaymentRepository, never()).saveAndFlush(any(PixPayment.class));
    }

    @Test
    void create_shouldThrowWhenReceivableAlreadyHasPixCharge() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(pixPaymentRepository.existsByReceivableId(receivable.getId())).thenReturn(true);

        Throwable thrown = catchThrowable(() -> service.create(request(receivable.getId())));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        verify(pixPaymentRepository, never()).saveAndFlush(any(PixPayment.class));
    }

    @Test
    void create_shouldThrow409OnConcurrentDuplicate() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(pixPaymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(pixPaymentRepository.saveAndFlush(any(PixPayment.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));

        Throwable thrown = catchThrowable(() -> service.create(request(receivable.getId())));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        assertThat(thrown.getMessage()).contains("already has a Pix charge");
    }

    @Test
    void confirm_shouldSettleReceivableAndMarkPaid() {
        var receivable = openReceivable(BigDecimal.valueOf(199.90));
        var pix = new PixPayment();
        pix.setId(UUID.randomUUID());
        pix.setReceivable(receivable);
        pix.setTxid("TXID123");
        pix.setCopyPaste("000201...");
        pix.setAmount(BigDecimal.valueOf(199.90));
        pix.setStatus(PixPaymentStatus.PENDING);
        pix.setExpiresAt(LocalDateTime.now().plusHours(24));

        when(pixPaymentRepository.findByTxid("TXID123")).thenReturn(Optional.of(pix));
        when(pixPaymentRepository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        PixPaymentResponse response = service.confirm("TXID123");

        assertThat(response.getStatus()).isEqualTo(PixPaymentStatus.PAID);
        assertThat(response.getPaidAt()).isNotNull();

        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).create(captor.capture());
        assertThat(captor.getValue().getReceivableId()).isEqualTo(receivable.getId());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo(PaymentMethodDto.PIX);
        assertThat(captor.getValue().getExternalReference()).isEqualTo("TXID123");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(199.90));
    }

    @Test
    void confirm_shouldThrowWhenPixNotFound() {
        when(pixPaymentRepository.findByTxid("UNKNOWN")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> service.confirm("UNKNOWN"));

        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        verify(paymentService, never()).create(any(PaymentRequest.class));
    }

    @Test
    void confirm_shouldThrowWhenNotPending() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        var pix = new PixPayment();
        pix.setReceivable(receivable);
        pix.setTxid("TXID");
        pix.setAmount(BigDecimal.valueOf(100));
        pix.setStatus(PixPaymentStatus.PAID);

        when(pixPaymentRepository.findByTxid("TXID")).thenReturn(Optional.of(pix));

        Throwable thrown = catchThrowable(() -> service.confirm("TXID"));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        verify(paymentService, never()).create(any(PaymentRequest.class));
    }

    @Test
    void confirm_shouldThrowAndExpireWhenExpired() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        var pix = new PixPayment();
        pix.setId(UUID.randomUUID());
        pix.setReceivable(receivable);
        pix.setTxid("TXID");
        pix.setAmount(BigDecimal.valueOf(100));
        pix.setStatus(PixPaymentStatus.PENDING);
        pix.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(pixPaymentRepository.findByTxid("TXID")).thenReturn(Optional.of(pix));
        when(pixPaymentRepository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        Throwable thrown = catchThrowable(() -> service.confirm("TXID"));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        assertThat(pix.getStatus()).isEqualTo(PixPaymentStatus.EXPIRED);
        verify(paymentService, never()).create(any(PaymentRequest.class));
    }

    @Test
    void confirm_shouldCancelWhenSettlementFails() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        var pix = new PixPayment();
        pix.setId(UUID.randomUUID());
        pix.setReceivable(receivable);
        pix.setTxid("TXID");
        pix.setAmount(BigDecimal.valueOf(100));
        pix.setStatus(PixPaymentStatus.PENDING);
        pix.setExpiresAt(LocalDateTime.now().plusHours(24));

        when(pixPaymentRepository.findByTxid("TXID")).thenReturn(Optional.of(pix));
        when(paymentService.create(any(PaymentRequest.class)))
                .thenThrow(new BusinessException("Receivable already has a payment"));
        when(pixPaymentRepository.save(any(PixPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        Throwable thrown = catchThrowable(() -> service.confirm("TXID"));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        assertThat(pix.getStatus()).isEqualTo(PixPaymentStatus.CANCELLED);
    }

    @Test
    void findById_shouldReturnPixCharge() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        var pix = new PixPayment();
        pix.setId(UUID.randomUUID());
        pix.setReceivable(receivable);
        pix.setTxid("TXID");
        pix.setCopyPaste("000201...");
        pix.setAmount(BigDecimal.valueOf(100));
        pix.setStatus(PixPaymentStatus.PENDING);

        when(pixPaymentRepository.findById(pix.getId())).thenReturn(Optional.of(pix));

        PixPaymentResponse response = service.findById(pix.getId());

        assertThat(response.getId()).isEqualTo(pix.getId());
        assertThat(response.getStudentName()).isEqualTo("Aluno Pix");
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(pixPaymentRepository.findById(id)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> service.findById(id));

        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAll_shouldReturnPixChargesOfStudio() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        var pix = new PixPayment();
        pix.setId(UUID.randomUUID());
        pix.setReceivable(receivable);
        pix.setTxid("TXID");
        pix.setCopyPaste("000201...");
        pix.setAmount(BigDecimal.valueOf(100));
        pix.setStatus(PixPaymentStatus.PENDING);

        var page = new PageImpl<>(List.of(pix), PageRequest.of(0, 10), 1);
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(pixPaymentRepository.findByStudioId(eq(studioId), any(PageRequest.class))).thenReturn(page);

        Page<PixPaymentResponse> result = service.findAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStudentName()).isEqualTo("Aluno Pix");
        assertThat(result.getContent().get(0).getTxid()).isEqualTo("TXID");
    }
}