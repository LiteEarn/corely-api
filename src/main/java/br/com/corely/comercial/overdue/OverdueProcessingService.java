package br.com.corely.comercial.overdue;

import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.shared.exception.ResourceNotFoundException;
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
                var outcome = transactionTemplate.execute(status -> markAsOverdue(invoice.getId()));
                if (outcome == OverdueResult.SUCCESS) {
                    result.incrementOverdue();
                } else {
                    result.incrementSkipped();
                }
            } catch (Exception e) {
                log.error("Error marking invoice {} as overdue: {}", invoice.getId(), e.getMessage(), e);
                result.incrementErrors();
            }
        }

        return result;
    }

    public OverdueResult markAsOverdue(UUID invoiceId) {
        var invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            log.warn("Invoice {} skipped: status is {} (expected PENDING)", invoiceId, invoice.getStatus());
            return OverdueResult.SKIPPED;
        }

        invoice.setStatus(InvoiceStatus.OVERDUE);
        invoiceRepository.save(invoice);
        return OverdueResult.SUCCESS;
    }

    enum OverdueResult {
        SUCCESS,
        SKIPPED
    }
}
