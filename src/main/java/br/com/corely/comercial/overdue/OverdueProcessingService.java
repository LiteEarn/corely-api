package br.com.corely.comercial.overdue;

import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class OverdueProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OverdueProcessingService.class);

    private final InvoiceRepository invoiceRepository;
    private final TransactionTemplate transactionTemplate;

    public OverdueProcessingService(InvoiceRepository invoiceRepository,
                                    TransactionTemplate transactionTemplate) {
        this.invoiceRepository = invoiceRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public OverdueProcessingResult process(LocalDate processingDate) {
        var result = new OverdueProcessingResult();
        var invoices = invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.PENDING, processingDate);

        for (var invoice : invoices) {
            result.incrementProcessed();
            try {
                transactionTemplate.execute(status -> {
                    markAsOverdue(invoice.getId(), result);
                    return null;
                });
            } catch (Exception e) {
                log.error("Error marking invoice {} as overdue: {}", invoice.getId(), e.getMessage(), e);
                result.incrementErrors();
            }
        }

        return result;
    }

    public void markAsOverdue(UUID invoiceId, OverdueProcessingResult result) {
        var invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            log.warn("Invoice {} skipped: status is {} (expected PENDING)", invoiceId, invoice.getStatus());
            result.incrementSkipped();
            return;
        }

        invoice.setStatus(InvoiceStatus.OVERDUE);
        invoiceRepository.save(invoice);
        result.incrementOverdue();
    }
}
