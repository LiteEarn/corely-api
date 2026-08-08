package br.com.corely.finance.cashflow;

import br.com.corely.finance.cashflow.dto.CashFlowBalanceResponse;
import br.com.corely.finance.cashflow.dto.CashFlowEntryRequest;
import br.com.corely.finance.cashflow.dto.CashFlowEntrySourceDto;
import br.com.corely.finance.cashflow.dto.CashFlowEntryTypeDto;
import br.com.corely.finance.payment.Payment;
import br.com.corely.finance.payment.PaymentMethod;
import br.com.corely.finance.payment.PaymentRepository;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.shared.tenant.TenantContext;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do serviço de fluxo de caixa — entradas (EPIC-03-S11).
 */
@ExtendWith(MockitoExtension.class)
class CashFlowEntryServiceTest {

    @Mock
    private CashFlowEntryRepository cashFlowEntryRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private TenantContext tenantContext;

    private CashFlowEntryService service;

    private UUID studioId;
    private Studio studio;

    @BeforeEach
    void setUp() {
        service = new CashFlowEntryService(cashFlowEntryRepository, paymentRepository,
                studioRepository, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);
    }

    private CashFlowEntryRequest manualEntryRequest() {
        var request = new CashFlowEntryRequest();
        request.setEntryType(CashFlowEntryTypeDto.ENTRY);
        request.setEntryDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(500));
        request.setDescription("Aporte inicial");
        request.setSource(CashFlowEntrySourceDto.MANUAL);
        request.setCategory("APORTE");
        return request;
    }

    @Test
    void create_shouldSaveManualEntry() {
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(cashFlowEntryRepository.save(any(CashFlowEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(manualEntryRequest());

        assertThat(response.getEntryType()).isEqualTo(CashFlowEntryTypeDto.ENTRY);
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(response.getSource()).isEqualTo(CashFlowEntrySourceDto.MANUAL);
        assertThat(response.getDescription()).isEqualTo("Aporte inicial");
        assertThat(response.getPaymentId()).isNull();
        verify(cashFlowEntryRepository).save(any(CashFlowEntry.class));
    }

    @Test
    void create_shouldSaveManualOutflow() {
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(cashFlowEntryRepository.save(any(CashFlowEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = manualEntryRequest();
        request.setEntryType(CashFlowEntryTypeDto.OUTFLOW);
        request.setDescription("Pagamento de fornecedor");
        request.setCategory("FORNECEDOR");

        var response = service.create(request);

        assertThat(response.getEntryType()).isEqualTo(CashFlowEntryTypeDto.OUTFLOW);
        assertThat(response.getDescription()).isEqualTo("Pagamento de fornecedor");
        assertThat(response.getPaymentId()).isNull();
        verify(cashFlowEntryRepository).save(any(CashFlowEntry.class));
    }

    @Test
    void create_shouldRejectOutflowSourcedFromPayment() {
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);

        var request = manualEntryRequest();
        request.setEntryType(CashFlowEntryTypeDto.OUTFLOW);
        request.setSource(CashFlowEntrySourceDto.PAYMENT);
        request.setPaymentId(UUID.randomUUID());

        Throwable thrown = catchThrowable(() -> service.create(request));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        assertThat(thrown.getMessage()).isEqualTo("OUTFLOW entries cannot be sourced from a payment");
        verify(cashFlowEntryRepository, never()).save(any());
    }

    @Test
    void create_shouldLinkPaymentWhenSourceIsPayment() {
        var payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setPaymentMethod(PaymentMethod.PIX);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(cashFlowEntryRepository.save(any(CashFlowEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = manualEntryRequest();
        request.setSource(CashFlowEntrySourceDto.PAYMENT);
        request.setPaymentId(payment.getId());

        var response = service.create(request);

        assertThat(response.getPaymentId()).isEqualTo(payment.getId());
        assertThat(response.getSource()).isEqualTo(CashFlowEntrySourceDto.PAYMENT);
    }

    @Test
    void create_shouldReturn400WhenPaymentSourceWithoutPaymentId() {
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);

        var request = manualEntryRequest();
        request.setSource(CashFlowEntrySourceDto.PAYMENT);

        Throwable thrown = catchThrowable(() -> service.create(request));

        assertThat(thrown).isInstanceOf(BusinessException.class);
        assertThat(thrown.getMessage()).isEqualTo("paymentId is required when source is PAYMENT");
        verify(cashFlowEntryRepository, never()).save(any());
    }

    @Test
    void create_shouldReturn404WhenPaymentNotFound() {
        var paymentId = UUID.randomUUID();
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        var request = manualEntryRequest();
        request.setSource(CashFlowEntrySourceDto.PAYMENT);
        request.setPaymentId(paymentId);

        Throwable thrown = catchThrowable(() -> service.create(request));

        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAll_shouldDelegateWithFilters() {
        var pageable = PageRequest.of(0, 10);
        var entry = new CashFlowEntry();
        entry.setId(UUID.randomUUID());
        entry.setEntryType(CashFlowEntryType.ENTRY);
        entry.setEntryDate(LocalDate.now());
        entry.setAmount(BigDecimal.valueOf(300));
        entry.setSource(CashFlowEntrySource.MANUAL);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(cashFlowEntryRepository.findByFilters(eq(studioId), eq(CashFlowEntryType.ENTRY),
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 12, 31)), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        Page<br.com.corely.finance.cashflow.dto.CashFlowEntryResponse> result =
                service.findAll(CashFlowEntryTypeDto.ENTRY,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEntryType()).isEqualTo(CashFlowEntryTypeDto.ENTRY);
    }

    @Test
    void getBalance_shouldComputeEntriesMinusOutflows() {
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(cashFlowEntryRepository.sumAmountByStudioIdAndTypeAndPeriod(
                eq(studioId), eq(CashFlowEntryType.ENTRY), isNull(), isNull()))
                .thenReturn(BigDecimal.valueOf(1000));
        when(cashFlowEntryRepository.sumAmountByStudioIdAndTypeAndPeriod(
                eq(studioId), eq(CashFlowEntryType.OUTFLOW), isNull(), isNull()))
                .thenReturn(BigDecimal.valueOf(400));

        CashFlowBalanceResponse balance = service.getBalance(null, null);

        assertThat(balance.getTotalEntries()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(balance.getTotalOutflows()).isEqualByComparingTo(BigDecimal.valueOf(400));
        assertThat(balance.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(600));
    }

    @Test
    void getBalance_shouldDelegatePeriodFilters() {
        var dateFrom = LocalDate.of(2026, 1, 1);
        var dateTo = LocalDate.of(2026, 12, 31);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(cashFlowEntryRepository.sumAmountByStudioIdAndTypeAndPeriod(
                eq(studioId), eq(CashFlowEntryType.ENTRY), eq(dateFrom), eq(dateTo)))
                .thenReturn(BigDecimal.ZERO);
        when(cashFlowEntryRepository.sumAmountByStudioIdAndTypeAndPeriod(
                eq(studioId), eq(CashFlowEntryType.OUTFLOW), eq(dateFrom), eq(dateTo)))
                .thenReturn(BigDecimal.ZERO);

        CashFlowBalanceResponse balance = service.getBalance(dateFrom, dateTo);

        assertThat(balance.getDateFrom()).isEqualTo(dateFrom);
        assertThat(balance.getDateTo()).isEqualTo(dateTo);
        assertThat(balance.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(cashFlowEntryRepository).sumAmountByStudioIdAndTypeAndPeriod(
                eq(studioId), eq(CashFlowEntryType.ENTRY), eq(dateFrom), eq(dateTo));
        verify(cashFlowEntryRepository).sumAmountByStudioIdAndTypeAndPeriod(
                eq(studioId), eq(CashFlowEntryType.OUTFLOW), eq(dateFrom), eq(dateTo));
    }

    @Test
    void findById_shouldReturn404WhenNotFound() {
        var id = UUID.randomUUID();
        when(cashFlowEntryRepository.findById(id)).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> service.findById(id));

        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
    }
}
