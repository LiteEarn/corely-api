package br.com.corely.comercial.overdue;

import br.com.corely.comercial.invoice.Invoice;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.shared.exception.ResourceNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void markAsOverdue_shouldReturnSuccessAndChangeStatus() {
        when(invoiceRepository.findById(pendingInvoiceId)).thenReturn(Optional.of(pendingInvoice));

        var result = service.markAsOverdue(pendingInvoiceId);

        assertThat(result).isEqualTo(OverdueProcessingService.OverdueResult.SUCCESS);
        assertThat(pendingInvoice.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
        verify(invoiceRepository).save(pendingInvoice);
    }

    @Test
    void markAsOverdue_shouldReturnSkippedWhenAlreadyPaid() {
        when(invoiceRepository.findById(paidInvoice.getId())).thenReturn(Optional.of(paidInvoice));

        var result = service.markAsOverdue(paidInvoice.getId());

        assertThat(result).isEqualTo(OverdueProcessingService.OverdueResult.SKIPPED);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void markAsOverdue_shouldReturnSkippedWhenAlreadyCancelled() {
        when(invoiceRepository.findById(cancelledInvoice.getId())).thenReturn(Optional.of(cancelledInvoice));

        var result = service.markAsOverdue(cancelledInvoice.getId());

        assertThat(result).isEqualTo(OverdueProcessingService.OverdueResult.SKIPPED);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void markAsOverdue_shouldReturnSkippedWhenAlreadyOverdue() {
        when(invoiceRepository.findById(overdueInvoice.getId())).thenReturn(Optional.of(overdueInvoice));

        var result = service.markAsOverdue(overdueInvoice.getId());

        assertThat(result).isEqualTo(OverdueProcessingService.OverdueResult.SKIPPED);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void markAsOverdue_shouldThrowResourceNotFoundException() {
        when(invoiceRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsOverdue(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invoice not found");
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
