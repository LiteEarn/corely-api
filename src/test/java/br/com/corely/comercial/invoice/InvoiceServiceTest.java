package br.com.corely.comercial.invoice;

import br.com.corely.comercial.contractsnapshot.ContractSnapshot;
import br.com.corely.comercial.invoice.dto.InvoiceRequest;
import br.com.corely.comercial.invoice.dto.InvoiceResponse;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
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
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private StudentPlanRepository studentPlanRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private ComercialTenantContext tenantContext;

    private InvoiceService service;

    private UUID studioId;
    private Studio studio;
    private Student student;
    private ContractSnapshot snapshot;
    private StudentPlan studentPlan;
    private UUID studentPlanId;

    @BeforeEach
    void setUp() {
        service = new InvoiceService(invoiceRepository, studentPlanRepository, studioRepository, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);

        student = new Student();
        student.setId(UUID.randomUUID());
        student.setFullName("Jane Doe");

        snapshot = new ContractSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setPlanName("Gold Plan");
        snapshot.setPlanPrice(BigDecimal.valueOf(199));
        snapshot.setPlanDuration(30);

        studentPlanId = UUID.randomUUID();
        studentPlan = new StudentPlan();
        studentPlan.setId(studentPlanId);
        studentPlan.setStudent(student);
        studentPlan.setContractSnapshot(snapshot);
        studentPlan.setStatus(StudentPlanStatus.ACTIVE);
    }

    @Test
    void create_shouldUseSnapshotPrice() {
        var request = new InvoiceRequest();
        request.setStudentPlanId(studentPlanId);
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setReferenceMonth("2026-08");

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentPlanRepository.findById(studentPlanId)).thenReturn(Optional.of(studentPlan));
        when(invoiceRepository.findByStudentPlanIdAndReferenceMonth(studentPlanId, "2026-08")).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            var inv2 = inv.getArgument(0, Invoice.class);
            inv2.setId(UUID.randomUUID());
            return inv2;
        });

        InvoiceResponse response = service.create(request);

        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("199"));
        assertThat(response.getPlanName()).isEqualTo("Gold Plan");
        assertThat(response.getStudentName()).isEqualTo("Jane Doe");
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void create_shouldThrowException_whenStudentPlanNotFound() {
        var request = new InvoiceRequest();
        request.setStudentPlanId(studentPlanId);
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setReferenceMonth("2026-08");

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentPlanRepository.findById(studentPlanId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("StudentPlan not found");
    }

    @Test
    void create_shouldThrowException_whenStudentPlanNotActive() {
        studentPlan.setStatus(StudentPlanStatus.SUSPENDED);

        var request = new InvoiceRequest();
        request.setStudentPlanId(studentPlanId);
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setReferenceMonth("2026-08");

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentPlanRepository.findById(studentPlanId)).thenReturn(Optional.of(studentPlan));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot create invoice for a non-active student plan.");
    }

    @Test
    void create_shouldThrowException_whenDuplicateMonth() {
        var request = new InvoiceRequest();
        request.setStudentPlanId(studentPlanId);
        request.setDueDate(LocalDate.of(2026, 8, 15));
        request.setReferenceMonth("2026-08");

        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(studentPlanRepository.findById(studentPlanId)).thenReturn(Optional.of(studentPlan));
        when(invoiceRepository.findByStudentPlanIdAndReferenceMonth(studentPlanId, "2026-08"))
                .thenReturn(Optional.of(new Invoice()));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invoice already exists for this month and student plan.");
    }

    @Test
    void cancel_shouldChangeStatus() {
        var invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStudentPlan(studentPlan);
        invoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse response = service.cancel(invoice.getId());

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrowException_whenNotPending() {
        var invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setStudentPlan(studentPlan);
        invoice.setStatus(InvoiceStatus.PAID);

        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.cancel(invoice.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Only PENDING invoices can be cancelled.");
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        when(invoiceRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invoice not found");
    }
}
