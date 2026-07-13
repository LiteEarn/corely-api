package br.com.corely.comercial.overdue;

import br.com.corely.comercial.invoice.Invoice;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OverdueProcessingServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private OverdueProcessingService service;

    private Invoice pendingInvoice;
    private UUID pendingInvoiceId;
    private Invoice paidInvoice;
    private Invoice cancelledInvoice;
    private Invoice overdueInvoice;

    @BeforeEach
    void setUp() {
        service = new OverdueProcessingService(invoiceRepository, transactionTemplate);

        pendingInvoiceId = UUID.randomUUID();
        pendingInvoice = new Invoice();
        pendingInvoice.setId(pendingInvoiceId);
        pendingInvoice.setAmount(BigDecimal.valueOf(299));
        pendingInvoice.setStatus(InvoiceStatus.PENDING);
        pendingInvoice.setDueDate(LocalDate.of(2026, 1, 15));

        paidInvoice = new Invoice();
        paidInvoice.setId(UUID.randomUUID());
        paidInvoice.setAmount(BigDecimal.valueOf(299));
        paidInvoice.setStatus(InvoiceStatus.PAID);
        paidInvoice.setDueDate(LocalDate.of(2026, 1, 15));

        cancelledInvoice = new Invoice();
        cancelledInvoice.setId(UUID.randomUUID());
        cancelledInvoice.setAmount(BigDecimal.valueOf(299));
        cancelledInvoice.setStatus(InvoiceStatus.CANCELLED);
        cancelledInvoice.setDueDate(LocalDate.of(2026, 1, 15));

        overdueInvoice = new Invoice();
        overdueInvoice.setId(UUID.randomUUID());
        overdueInvoice.setAmount(BigDecimal.valueOf(299));
        overdueInvoice.setStatus(InvoiceStatus.OVERDUE);
        overdueInvoice.setDueDate(LocalDate.of(2026, 1, 15));
    }

    @Test
    void markAsOverdue_shouldChangeStatus() {
        var result = new OverdueProcessingResult();

        when(invoiceRepository.findById(pendingInvoiceId)).thenReturn(Optional.of(pendingInvoice));

        service.markAsOverdue(pendingInvoiceId, result);

        assertThat(pendingInvoice.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
        assertThat(result.getOverdue()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);

        verify(invoiceRepository).save(pendingInvoice);
    }

    @Test
    void markAsOverdue_shouldSkipWhenAlreadyPaid() {
        var result = new OverdueProcessingResult();

        when(invoiceRepository.findById(paidInvoice.getId())).thenReturn(Optional.of(paidInvoice));

        service.markAsOverdue(paidInvoice.getId(), result);

        assertThat(result.getOverdue()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void markAsOverdue_shouldSkipWhenAlreadyCancelled() {
        var result = new OverdueProcessingResult();

        when(invoiceRepository.findById(cancelledInvoice.getId())).thenReturn(Optional.of(cancelledInvoice));

        service.markAsOverdue(cancelledInvoice.getId(), result);

        assertThat(result.getOverdue()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void markAsOverdue_shouldSkipWhenAlreadyOverdue() {
        var result = new OverdueProcessingResult();

        when(invoiceRepository.findById(overdueInvoice.getId())).thenReturn(Optional.of(overdueInvoice));

        service.markAsOverdue(overdueInvoice.getId(), result);

        assertThat(result.getOverdue()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void process_shouldFindPendingInvoicesAndMarkOverdue() {
        var processingDate = LocalDate.of(2026, 2, 1);

        when(invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.PENDING, processingDate))
                .thenReturn(List.of(pendingInvoice));
        when(invoiceRepository.findById(pendingInvoiceId)).thenReturn(Optional.of(pendingInvoice));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            var callback = inv.getArgument(0, org.springframework.transaction.support.TransactionCallback.class);
            return callback.doInTransaction(null);
        });

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(1);
        assertThat(result.getOverdue()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
    }

    @Test
    void process_shouldHandleErrorWithoutInterruptingOthers() {
        var processingDate = LocalDate.of(2026, 2, 1);

        when(invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.PENDING, processingDate))
                .thenReturn(List.of(pendingInvoice, paidInvoice));
        when(transactionTemplate.execute(any())).thenThrow(new RuntimeException("DB error"));

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(2);
        assertThat(result.getErrors()).isEqualTo(2);
    }

    @Test
    void process_shouldNotProcessWhenNoOverdueInvoices() {
        var processingDate = LocalDate.of(2026, 2, 1);

        when(invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.PENDING, processingDate))
                .thenReturn(List.of());

        var result = service.process(processingDate);

        assertThat(result.getProcessed()).isEqualTo(0);
        assertThat(result.getOverdue()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getErrors()).isEqualTo(0);
    }
}
