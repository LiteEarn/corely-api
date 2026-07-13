package br.com.corely.comercial.payment;

import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.comercial.invoice.Invoice;
import br.com.corely.comercial.invoice.InvoiceRepository;
import br.com.corely.comercial.invoice.InvoiceStatus;
import br.com.corely.comercial.payment.dto.PaymentMethodDto;
import br.com.corely.comercial.payment.dto.PaymentRequest;
import br.com.corely.comercial.payment.dto.PaymentResponse;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private ComercialTenantContext tenantContext;

    private PaymentService service;

    private UUID studioId;
    private Studio studio;
    private Invoice invoice;
    private UUID invoiceId;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, invoiceRepository, studioRepository, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);

        var student = new Student();
        student.setId(UUID.randomUUID());
        student.setFullName("Jane Doe");

        var snapshot = new ContractSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setPlanName("Gold Plan");
        snapshot.setPlanPrice(BigDecimal.valueOf(299));

        var studentPlan = new StudentPlan();
        studentPlan.setId(UUID.randomUUID());
        studentPlan.setStudent(student);
        studentPlan.setContractSnapshot(snapshot);

        invoiceId = UUID.randomUUID();
        invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setStudentPlan(studentPlan);
        invoice.setAmount(BigDecimal.valueOf(299));
        invoice.setStatus(InvoiceStatus.PENDING);
    }

    @Test
    void create_shouldRegisterPaymentAndChangeInvoiceToPaid() {
        var request = new PaymentRequest();
        request.setInvoiceId(invoiceId);
        request.setPaymentDate(LocalDate.of(2026, 8, 15));
        request.setAmount(BigDecimal.valueOf(299));
        request.setPaymentMethod(PaymentMethodDto.PIX);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(invoiceId)).thenReturn(Optional.empty());

        var savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setInvoice(invoice);
        savedPayment.setPaymentDate(request.getPaymentDate());
        savedPayment.setAmount(request.getAmount());
        savedPayment.setPaymentMethod(PaymentMethod.PIX);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponse response = service.create(request);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("299"));
    }

    @Test
    void create_shouldThrowException_whenInvoiceNotFound() {
        var request = new PaymentRequest();
        request.setInvoiceId(invoiceId);
        request.setPaymentDate(LocalDate.of(2026, 8, 15));
        request.setAmount(BigDecimal.valueOf(299));
        request.setPaymentMethod(PaymentMethodDto.PIX);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invoice not found");
    }

    @Test
    void create_shouldThrowException_whenInvoiceNotPending() {
        invoice.setStatus(InvoiceStatus.PAID);

        var request = new PaymentRequest();
        request.setInvoiceId(invoiceId);
        request.setPaymentDate(LocalDate.of(2026, 8, 15));
        request.setAmount(BigDecimal.valueOf(299));
        request.setPaymentMethod(PaymentMethodDto.PIX);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Only PENDING invoices can be paid.");
    }

    @Test
    void create_shouldThrowException_whenDuplicatePayment() {
        var request = new PaymentRequest();
        request.setInvoiceId(invoiceId);
        request.setPaymentDate(LocalDate.of(2026, 8, 15));
        request.setAmount(BigDecimal.valueOf(299));
        request.setPaymentMethod(PaymentMethodDto.PIX);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(invoiceId)).thenReturn(Optional.of(new Payment()));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invoice already has a payment.");
    }

    @Test
    void create_shouldThrowException_whenAmountMismatch() {
        var request = new PaymentRequest();
        request.setInvoiceId(invoiceId);
        request.setPaymentDate(LocalDate.of(2026, 8, 15));
        request.setAmount(BigDecimal.valueOf(300));
        request.setPaymentMethod(PaymentMethodDto.PIX);

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Payment amount must equal invoice amount.");
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        when(paymentRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found");
    }
}
