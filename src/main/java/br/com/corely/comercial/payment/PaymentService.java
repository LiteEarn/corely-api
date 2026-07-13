package br.com.corely.comercial.payment;

import br.com.corely.comercial.invoice.Invoice;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.comercial.payment.dto.PaymentMethodDto;
import br.com.corely.comercial.payment.dto.PaymentRequest;
import br.com.corely.comercial.payment.dto.PaymentResponse;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException("Only PENDING invoices can be paid.");
        }

        if (paymentRepository.findByInvoiceId(request.getInvoiceId()).isPresent()) {
            throw new BusinessException("Invoice already has a payment.");
        }

        if (request.getAmount().compareTo(invoice.getAmount()) != 0) {
            throw new BusinessException("Payment amount must equal invoice amount.");
        }

        var payment = new Payment();
        payment.setStudio(studio);
        payment.setInvoice(invoice);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().name()));
        payment.setExternalReference(request.getExternalReference());
        payment.setNotes(request.getNotes());

        payment = paymentRepository.save(payment);

        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID id) {
        var payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll() {
        return paymentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentResponse toResponse(Payment payment) {
        var invoice = payment.getInvoice();
        var sp = invoice.getStudentPlan();
        var student = sp.getStudent();
        var snapshot = sp.getContractSnapshot();
        return new PaymentResponse(
                payment.getId(),
                invoice.getId(),
                student.getFullName(),
                snapshot.getPlanName(),
                payment.getPaymentDate(),
                payment.getAmount(),
                PaymentMethodDto.valueOf(payment.getPaymentMethod().name()),
                payment.getExternalReference(),
                payment.getNotes(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
