package br.com.corely.finance.invoice;

import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.finance.invoice.dto.InvoiceRequest;
import br.com.corely.finance.invoice.dto.InvoiceResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service("financeInvoiceService")
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final StudentRepository studentRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        var invoice = new Invoice();
        invoice.setStudio(studio);
        invoice.setStudent(student);
        invoice.setDueDate(request.getDueDate());
        invoice.setAmount(request.getAmount());
        invoice.setDescription(request.getDescription());
        invoice.setStatus(InvoiceStatus.PENDING);

        invoice = invoiceRepository.save(invoice);
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(UUID id) {
        var invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll() {
        return invoiceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InvoiceResponse pay(UUID id) {
        var invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("Cannot pay a cancelled invoice");
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("Invoice is already paid");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentDate(LocalDate.now());
        invoice = invoiceRepository.save(invoice);
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse cancel(UUID id) {
        var invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("Cannot cancel a paid invoice");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            return toResponse(invoice);
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice = invoiceRepository.save(invoice);
        return toResponse(invoice);
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getStudent().getId(),
                invoice.getStudent().getFullName(),
                invoice.getDueDate(),
                invoice.getAmount(),
                invoice.getDescription(),
                invoice.getStatus(),
                invoice.getPaymentDate(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }
}
