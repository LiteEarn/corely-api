package br.com.corely.finance.card;

import br.com.corely.finance.payment.PaymentRepository;
import br.com.corely.finance.payment.PaymentService;
import br.com.corely.finance.payment.dto.PaymentMethodDto;
import br.com.corely.finance.payment.dto.PaymentRequest;
import br.com.corely.finance.card.dto.CardPaymentRequest;
import br.com.corely.finance.card.dto.CardPaymentResponse;
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
 * Testes unitários do serviço de transações de cartão (EPIC-03-S08).
 */
@ExtendWith(MockitoExtension.class)
class CardPaymentServiceTest {

    @Mock
    private CardPaymentRepository cardPaymentRepository;

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

    private CardPaymentService service;

    private UUID studioId;
    private Studio studio;
    private Student student;

    @BeforeEach
    void setUp() {
        service = new CardPaymentService(cardPaymentRepository, receivableRepository, paymentRepository,
                paymentService, studioRepository, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);

        student = new Student();
        student.setId(UUID.randomUUID());
        student.setFullName("Aluno Cartão");
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

    private CardPaymentRequest request(UUID receivableId) {
        var request = new CardPaymentRequest();
        request.setReceivableId(receivableId);
        request.setCardBrand("VISA");
        request.setLastFourDigits("1234");
        request.setInstallments(1);
        return request;
    }

    @Test
    void create_shouldGenerateCardTransaction() {
        var receivable = openReceivable(BigDecimal.valueOf(199.90));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(cardPaymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(cardPaymentRepository.saveAndFlush(any(CardPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        CardPaymentResponse response = service.create(request(receivable.getId()));

        assertThat(response.getReceivableId()).isEqualTo(receivable.getId());
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(199.90));
        assertThat(response.getStatus()).isEqualTo(CardPaymentStatus.PENDING);
        assertThat(response.getTransactionId()).hasSize(32);
        assertThat(response.getCardBrand()).isEqualTo("VISA");
        assertThat(response.getLastFourDigits()).isEqualTo("1234");
        assertThat(response.getInstallments()).isEqualTo(1);
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
        verify(cardPaymentRepository, never()).saveAndFlush(any(CardPayment.class));
    }

    @Test
    void create_shouldThrowWhenReceivableHasPayment() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(true);

        Throwable thrown = catchThrowable(() -> service.create(request(receivable.getId())));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        verify(cardPaymentRepository, never()).saveAndFlush(any(CardPayment.class));
    }

    @Test
    void create_shouldThrowWhenReceivableAlreadyHasCardTransaction() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(cardPaymentRepository.existsByReceivableId(receivable.getId())).thenReturn(true);

        Throwable thrown = catchThrowable(() -> service.create(request(receivable.getId())));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        verify(cardPaymentRepository, never()).saveAndFlush(any(CardPayment.class));
    }

    @Test
    void create_shouldThrow409OnConcurrentDuplicate() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(receivableRepository.findById(receivable.getId())).thenReturn(Optional.of(receivable));
        when(paymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(cardPaymentRepository.existsByReceivableId(receivable.getId())).thenReturn(false);
        when(cardPaymentRepository.saveAndFlush(any(CardPayment.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));

        Throwable thrown = catchThrowable(() -> service.create(request(receivable.getId())));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        assertThat(thrown.getMessage()).contains("already has a card transaction");
    }

    @Test
    void confirm_shouldSettleReceivableAndMarkPaid() {
        var receivable = openReceivable(BigDecimal.valueOf(199.90));
        var card = new CardPayment();
        card.setId(UUID.randomUUID());
        card.setReceivable(receivable);
        card.setTransactionId("TXN123");
        card.setCardBrand("VISA");
        card.setLastFourDigits("1234");
        card.setInstallments(1);
        card.setAmount(BigDecimal.valueOf(199.90));
        card.setStatus(CardPaymentStatus.PENDING);
        card.setExpiresAt(LocalDateTime.now().plusHours(24));

        when(cardPaymentRepository.findByTransactionId("TXN123")).thenReturn(Optional.of(card));
        when(cardPaymentRepository.save(any(CardPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        CardPaymentResponse response = service.confirm("TXN123");

        assertThat(response.getStatus()).isEqualTo(CardPaymentStatus.PAID);
        assertThat(response.getPaidAt()).isNotNull();

        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).create(captor.capture());
        assertThat(captor.getValue().getReceivableId()).isEqualTo(receivable.getId());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo(PaymentMethodDto.CREDIT_CARD);
        assertThat(captor.getValue().getExternalReference()).isEqualTo("TXN123");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(199.90));
    }

    @Test
    void confirm_shouldThrowWhenCardNotFound() {
        when(cardPaymentRepository.findByTransactionId("UNKNOWN")).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> service.confirm("UNKNOWN"));

        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        verify(paymentService, never()).create(any(PaymentRequest.class));
    }

    @Test
    void confirm_shouldThrowWhenNotPending() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        var card = new CardPayment();
        card.setReceivable(receivable);
        card.setTransactionId("TXN");
        card.setAmount(BigDecimal.valueOf(100));
        card.setStatus(CardPaymentStatus.PAID);

        when(cardPaymentRepository.findByTransactionId("TXN")).thenReturn(Optional.of(card));

        Throwable thrown = catchThrowable(() -> service.confirm("TXN"));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        verify(paymentService, never()).create(any(PaymentRequest.class));
    }

    @Test
    void confirm_shouldThrowAndExpireWhenExpired() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        var card = new CardPayment();
        card.setId(UUID.randomUUID());
        card.setReceivable(receivable);
        card.setTransactionId("TXN");
        card.setAmount(BigDecimal.valueOf(100));
        card.setStatus(CardPaymentStatus.PENDING);
        card.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(cardPaymentRepository.findByTransactionId("TXN")).thenReturn(Optional.of(card));
        when(cardPaymentRepository.save(any(CardPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        Throwable thrown = catchThrowable(() -> service.confirm("TXN"));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        assertThat(card.getStatus()).isEqualTo(CardPaymentStatus.EXPIRED);
        verify(paymentService, never()).create(any(PaymentRequest.class));
    }

    @Test
    void confirm_shouldCancelWhenSettlementFails() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        var card = new CardPayment();
        card.setId(UUID.randomUUID());
        card.setReceivable(receivable);
        card.setTransactionId("TXN");
        card.setAmount(BigDecimal.valueOf(100));
        card.setStatus(CardPaymentStatus.PENDING);
        card.setExpiresAt(LocalDateTime.now().plusHours(24));

        when(cardPaymentRepository.findByTransactionId("TXN")).thenReturn(Optional.of(card));
        when(paymentService.create(any(PaymentRequest.class)))
                .thenThrow(new BusinessException("Receivable already has a payment"));
        when(cardPaymentRepository.save(any(CardPayment.class))).thenAnswer(inv -> inv.getArgument(0));

        Throwable thrown = catchThrowable(() -> service.confirm("TXN"));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        assertThat(card.getStatus()).isEqualTo(CardPaymentStatus.CANCELLED);
    }

    @Test
    void findById_shouldReturnCardTransaction() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        var card = new CardPayment();
        card.setId(UUID.randomUUID());
        card.setReceivable(receivable);
        card.setTransactionId("TXN");
        card.setCardBrand("MASTER");
        card.setLastFourDigits("4321");
        card.setInstallments(2);
        card.setAmount(BigDecimal.valueOf(100));
        card.setStatus(CardPaymentStatus.PENDING);

        when(cardPaymentRepository.findById(card.getId())).thenReturn(Optional.of(card));

        CardPaymentResponse response = service.findById(card.getId());

        assertThat(response.getId()).isEqualTo(card.getId());
        assertThat(response.getStudentName()).isEqualTo("Aluno Cartão");
        assertThat(response.getCardBrand()).isEqualTo("MASTER");
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(cardPaymentRepository.findById(id)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> service.findById(id));

        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAll_shouldReturnCardTransactionsOfStudio() {
        var receivable = openReceivable(BigDecimal.valueOf(100));
        var card = new CardPayment();
        card.setId(UUID.randomUUID());
        card.setReceivable(receivable);
        card.setTransactionId("TXN");
        card.setCardBrand("VISA");
        card.setLastFourDigits("1234");
        card.setInstallments(1);
        card.setAmount(BigDecimal.valueOf(100));
        card.setStatus(CardPaymentStatus.PENDING);

        var page = new PageImpl<>(List.of(card), PageRequest.of(0, 10), 1);
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(cardPaymentRepository.findByStudioId(eq(studioId), any(PageRequest.class))).thenReturn(page);

        Page<CardPaymentResponse> result = service.findAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStudentName()).isEqualTo("Aluno Cartão");
        assertThat(result.getContent().get(0).getTransactionId()).isEqualTo("TXN");
    }
}