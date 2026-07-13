package br.com.corely.comercial.invoice;

import br.com.corely.comercial.invoice.dto.InvoiceRequest;
import br.com.corely.comercial.invoice.dto.InvoiceResponse;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final StudentPlanRepository studentPlanRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var studentPlan = studentPlanRepository.findById(request.getStudentPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("StudentPlan not found"));

        if (studentPlan.getStatus() != StudentPlanStatus.ACTIVE) {
            throw new BusinessException("Cannot create invoice for a non-active student plan.");
        }

        if (invoiceRepository.findByStudentPlanIdAndReferenceMonth(
                request.getStudentPlanId(), request.getReferenceMonth()).isPresent()) {
            throw new BusinessException("Invoice already exists for this month and student plan.");
        }

        var snapshot = studentPlan.getContractSnapshot();

        var invoice = new Invoice();
        invoice.setStudio(studio);
        invoice.setStudentPlan(studentPlan);
        invoice.setDueDate(request.getDueDate());
        invoice.setReferenceMonth(request.getReferenceMonth());
        invoice.setAmount(snapshot.getPlanPrice());
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setIssueDate(LocalDate.now());

        invoice = invoiceRepository.save(invoice);
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse cancel(UUID id) {
        var invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException("Only PENDING invoices can be cancelled.");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
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
        return invoiceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        var sp = invoice.getStudentPlan();
        var student = sp.getStudent();
        var snapshot = sp.getContractSnapshot();
        return new InvoiceResponse(
                invoice.getId(),
                sp.getId(),
                student.getFullName(),
                snapshot.getPlanName(),
                invoice.getDueDate(),
                invoice.getReferenceMonth(),
                invoice.getAmount(),
                invoice.getStatus(),
                invoice.getIssueDate(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }
}
