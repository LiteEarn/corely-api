package br.com.corely.finance.invoice;

import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.finance.invoice.dto.InvoiceRequest;
import br.com.corely.finance.invoice.dto.InvoiceResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private ComercialTenantContext tenantContext;

    private InvoiceService service;

    private UUID studioId;
    private Studio studio;
    private Student student;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        service = new InvoiceService(invoiceRepository, studentRepository, studioRepository, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);

        studentId = UUID.randomUUID();
        student = new Student();
        student.setId(studentId);
        student.setFullName("John Doe");
        student.setActive(true);
    }

    @Test
    void create_shouldPersistInvoice() {
        var request = new InvoiceRequest();
        request.setStudentId(studentId);
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setAmount(BigDecimal.valueOf(150));
        request.setDescription("Mensalidade Agosto");

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            var inv2 = inv.getArgument(0, Invoice.class);
            inv2.setId(UUID.randomUUID());
            return inv2;
        });

        InvoiceResponse response = service.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(studentId);
        assertThat(response.getStudentName()).isEqualTo("John Doe");
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(response.getDescription()).isEqualTo("Mensalidade Agosto");
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(response.getPaymentDate()).isNull();
    }

    @Test
    void create_shouldThrowException_whenStudentNotFound() {
        var request = new InvoiceRequest();
        request.setStudentId(studentId);
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setAmount(BigDecimal.valueOf(150));

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Student not found");
    }

    @Test
    void pay_shouldChangeStatusToPaidAndSetPaymentDate() {
        var invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStudent(student);
        invoice.setDueDate(LocalDate.of(2026, 8, 15));
        invoice.setAmount(BigDecimal.valueOf(150));
        invoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse response = service.pay(invoice.getId());

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(response.getPaymentDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void pay_shouldThrowException_whenAlreadyPaid() {
        var invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStudent(student);
        invoice.setStatus(InvoiceStatus.PAID);

        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.pay(invoice.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invoice is already paid");
    }

    @Test
    void pay_shouldThrowException_whenCancelled() {
        var invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStudent(student);
        invoice.setStatus(InvoiceStatus.CANCELLED);

        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.pay(invoice.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot pay a cancelled invoice");
    }

    @Test
    void cancel_shouldChangeStatus() {
        var invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStudent(student);
        invoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse response = service.cancel(invoice.getId());

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrowException_whenPaid() {
        var invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStudent(student);
        invoice.setStatus(InvoiceStatus.PAID);

        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.cancel(invoice.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot cancel a paid invoice");
    }

    @Test
    void cancel_shouldBeIdempotent_whenAlreadyCancelled() {
        var invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStudent(student);
        invoice.setStatus(InvoiceStatus.CANCELLED);

        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        InvoiceResponse response = service.cancel(invoice.getId());

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void findById_shouldReturnInvoice() {
        var invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStudent(student);
        invoice.setDueDate(LocalDate.of(2026, 8, 15));
        invoice.setAmount(BigDecimal.valueOf(150));
        invoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        InvoiceResponse response = service.findById(invoice.getId());

        assertThat(response.getId()).isEqualTo(invoice.getId());
        assertThat(response.getStudentName()).isEqualTo("John Doe");
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        when(invoiceRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invoice not found");
    }

    @Test
    void findAll_shouldReturnAllInvoices() {
        var invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStudent(student);
        invoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice));

        var all = service.findAll();

        assertThat(all).hasSize(1);
    }

    @Test
    void pay_shouldThrowException_whenInvoiceNotFound() {
        when(invoiceRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pay(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invoice not found");
    }

    @Test
    void cancel_shouldThrowException_whenInvoiceNotFound() {
        when(invoiceRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invoice not found");
    }
}
