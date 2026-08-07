package br.com.corely.finance.cash;

import br.com.corely.finance.cash.dto.CashPaymentRequest;
import br.com.corely.finance.payment.PaymentService;
import br.com.corely.finance.payment.dto.PaymentMethodDto;
import br.com.corely.finance.payment.dto.PaymentRequest;
import br.com.corely.finance.payment.dto.PaymentResponse;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do serviço de pagamentos em dinheiro (EPIC-03-S09).
 */
@ExtendWith(MockitoExtension.class)
class CashPaymentServiceTest {

    @Mock
    private PaymentService paymentService;

    private CashPaymentService service;

    @BeforeEach
    void setUp() {
        service = new CashPaymentService(paymentService);
    }

    @Test
    void create_shouldDelegateToPaymentServiceWithCashMethod() {
        var request = new CashPaymentRequest();
        request.setReceivableId(UUID.randomUUID());
        request.setPaymentDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(150));
        request.setNotes("Recebido em dinheiro");

        var expected = new PaymentResponse(UUID.randomUUID(), request.getReceivableId(), null,
                UUID.randomUUID(), "Cash Student", request.getPaymentDate(), request.getAmount(),
                PaymentMethodDto.CASH, null, request.getNotes(), null, null);
        when(paymentService.create(any(PaymentRequest.class))).thenReturn(expected);

        var result = service.create(request);

        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethodDto.CASH);

        var captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).create(captor.capture());
        var captured = captor.getValue();
        assertThat(captured.getReceivableId()).isEqualTo(request.getReceivableId());
        assertThat(captured.getInstallmentId()).isNull();
        assertThat(captured.getPaymentDate()).isEqualTo(request.getPaymentDate());
        assertThat(captured.getAmount()).isEqualByComparingTo(request.getAmount());
        assertThat(captured.getPaymentMethod()).isEqualTo(PaymentMethodDto.CASH);
        assertThat(captured.getNotes()).isEqualTo("Recebido em dinheiro");
    }

    @Test
    void create_shouldForwardInstallmentId() {
        var request = new CashPaymentRequest();
        request.setReceivableId(UUID.randomUUID());
        request.setInstallmentId(UUID.randomUUID());
        request.setPaymentDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(50));

        when(paymentService.create(any(PaymentRequest.class))).thenReturn(new PaymentResponse());

        service.create(request);

        var captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).create(captor.capture());
        assertThat(captor.getValue().getInstallmentId()).isEqualTo(request.getInstallmentId());
    }

    @Test
    void findAll_shouldDelegateToPaymentServiceByCashMethod() {
        var pageable = PageRequest.of(0, 10);
        var payment = new PaymentResponse(UUID.randomUUID(), UUID.randomUUID(), null,
                UUID.randomUUID(), "Cash Student", LocalDate.now(), BigDecimal.valueOf(120),
                PaymentMethodDto.CASH, null, null, null, null);
        Page<PaymentResponse> page = new PageImpl<>(List.of(payment));

        when(paymentService.findAllByMethod(eq(PaymentMethodDto.CASH), eq(pageable))).thenReturn(page);

        var result = service.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(paymentService).findAllByMethod(PaymentMethodDto.CASH, pageable);
    }
}