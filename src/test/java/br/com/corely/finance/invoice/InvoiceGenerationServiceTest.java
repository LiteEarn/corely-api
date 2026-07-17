package br.com.corely.finance.invoice;

import br.com.corely.billingconfiguration.BillingConfiguration;
import br.com.corely.billingconfiguration.BillingConfigurationRepository;
import br.com.corely.finance.invoice.dto.GenerateInvoiceRequest;
import br.com.corely.finance.invoice.dto.GenerateInvoiceResponse;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceGenerationServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private BillingConfigurationRepository billingConfigurationRepository;

    @Captor
    private ArgumentCaptor<List<Invoice>> invoiceListCaptor;

    private InvoiceGenerationService service;

    private UUID studioId;
    private Studio studio;
    private BillingConfiguration config;
    private Student activeStudent;
    private Student studentWithBillingDisabled;
    private Student inactiveStudent;

    @BeforeEach
    void setUp() {
        service = new InvoiceGenerationService(invoiceRepository, studentRepository, billingConfigurationRepository);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);

        config = new BillingConfiguration();
        config.setStudio(studio);
        config.setDueDay(15);
        config.setDefaultAmount(BigDecimal.valueOf(199));
        config.setActive(true);

        activeStudent = new Student();
        activeStudent.setId(UUID.randomUUID());
        activeStudent.setFullName("Active Student");
        activeStudent.setActive(true);
        activeStudent.setBillingEnabled(true);

        studentWithBillingDisabled = new Student();
        studentWithBillingDisabled.setId(UUID.randomUUID());
        studentWithBillingDisabled.setFullName("No Billing");
        studentWithBillingDisabled.setActive(true);
        studentWithBillingDisabled.setBillingEnabled(false);

        inactiveStudent = new Student();
        inactiveStudent.setId(UUID.randomUUID());
        inactiveStudent.setFullName("Inactive");
        inactiveStudent.setActive(false);
        inactiveStudent.setBillingEnabled(true);
    }

    @Test
    void generate_shouldCreateInvoicesForActiveBillingEnabledStudents() {
        when(billingConfigurationRepository.findByStudioId(studioId)).thenReturn(Optional.of(config));
        when(studentRepository.findByStudioIdAndActiveTrue(studioId)).thenReturn(List.of(activeStudent));
        when(invoiceRepository.findByStudentIdAndBillingMonthAndStudioId(
                activeStudent.getId(), "2026-08", studioId)).thenReturn(Optional.empty());

        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-08");

        GenerateInvoiceResponse response = service.generate(request, studioId);

        assertThat(response.getCreated()).isEqualTo(1);
        assertThat(response.getIgnored()).isEqualTo(0);

        verify(invoiceRepository).saveAll(invoiceListCaptor.capture());
        var saved = invoiceListCaptor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getStudent().getId()).isEqualTo(activeStudent.getId());
        assertThat(saved.get(0).getBillingMonth()).isEqualTo("2026-08");
        assertThat(saved.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(199));
        assertThat(saved.get(0).getDueDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 15));
        assertThat(saved.get(0).getStatus()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void generate_shouldSkipStudentWithBillingDisabled() {
        when(billingConfigurationRepository.findByStudioId(studioId)).thenReturn(Optional.of(config));
        when(studentRepository.findByStudioIdAndActiveTrue(studioId)).thenReturn(List.of(activeStudent, studentWithBillingDisabled));
        when(invoiceRepository.findByStudentIdAndBillingMonthAndStudioId(
                any(), eq("2026-08"), eq(studioId))).thenReturn(Optional.empty());

        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-08");

        GenerateInvoiceResponse response = service.generate(request, studioId);

        assertThat(response.getCreated()).isEqualTo(1);
        assertThat(response.getIgnored()).isEqualTo(1);

        verify(invoiceRepository).saveAll(invoiceListCaptor.capture());
        assertThat(invoiceListCaptor.getValue()).hasSize(1);
        assertThat(invoiceListCaptor.getValue().get(0).getStudent().getId()).isEqualTo(activeStudent.getId());
    }

    @Test
    void generate_shouldSkipInactiveStudents() {
        when(billingConfigurationRepository.findByStudioId(studioId)).thenReturn(Optional.of(config));
        when(studentRepository.findByStudioIdAndActiveTrue(studioId)).thenReturn(List.of(activeStudent));

        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-08");

        GenerateInvoiceResponse response = service.generate(request, studioId);

        assertThat(response.getCreated()).isEqualTo(1);
        assertThat(response.getIgnored()).isEqualTo(0);
    }

    @Test
    void generate_shouldSkipWhenInvoiceAlreadyExists() {
        var existingInvoice = new Invoice();
        existingInvoice.setId(UUID.randomUUID());

        when(billingConfigurationRepository.findByStudioId(studioId)).thenReturn(Optional.of(config));
        when(studentRepository.findByStudioIdAndActiveTrue(studioId)).thenReturn(List.of(activeStudent));
        when(invoiceRepository.findByStudentIdAndBillingMonthAndStudioId(
                activeStudent.getId(), "2026-08", studioId)).thenReturn(Optional.of(existingInvoice));

        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-08");

        GenerateInvoiceResponse response = service.generate(request, studioId);

        assertThat(response.getCreated()).isEqualTo(0);
        assertThat(response.getIgnored()).isEqualTo(1);

        verify(invoiceRepository, never()).saveAll(any());
    }

    @Test
    void generate_shouldOnlyProcessSameStudioStudents() {
        var otherStudio = new Studio();
        otherStudio.setId(UUID.randomUUID());
        var studentFromOtherStudio = new Student();
        studentFromOtherStudio.setId(UUID.randomUUID());
        studentFromOtherStudio.setActive(true);
        studentFromOtherStudio.setBillingEnabled(true);

        when(billingConfigurationRepository.findByStudioId(studioId)).thenReturn(Optional.of(config));
        when(studentRepository.findByStudioIdAndActiveTrue(studioId)).thenReturn(List.of(activeStudent));
        when(invoiceRepository.findByStudentIdAndBillingMonthAndStudioId(
                activeStudent.getId(), "2026-08", studioId)).thenReturn(Optional.empty());

        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-08");

        GenerateInvoiceResponse response = service.generate(request, studioId);

        assertThat(response.getCreated()).isEqualTo(1);
        assertThat(response.getIgnored()).isEqualTo(0);
    }

    @Test
    void generate_shouldThrowWhenConfigNotFound() {
        when(billingConfigurationRepository.findByStudioId(studioId)).thenReturn(Optional.empty());

        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-08");

        assertThatThrownBy(() -> service.generate(request, studioId))
                .isInstanceOf(br.com.corely.shared.exception.ResourceNotFoundException.class);
    }

    @Test
    void generate_shouldHandleDifferentMonths() {
        when(billingConfigurationRepository.findByStudioId(studioId)).thenReturn(Optional.of(config));
        when(studentRepository.findByStudioIdAndActiveTrue(studioId)).thenReturn(List.of(activeStudent));
        when(invoiceRepository.findByStudentIdAndBillingMonthAndStudioId(
                activeStudent.getId(), "2026-09", studioId)).thenReturn(Optional.empty());

        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-09");

        GenerateInvoiceResponse response = service.generate(request, studioId);

        assertThat(response.getCreated()).isEqualTo(1);

        verify(invoiceRepository).saveAll(invoiceListCaptor.capture());
        assertThat(invoiceListCaptor.getValue().get(0).getBillingMonth()).isEqualTo("2026-09");
        assertThat(invoiceListCaptor.getValue().get(0).getDueDate()).isEqualTo(java.time.LocalDate.of(2026, 9, 15));
    }

    @Test
    void generate_shouldUseBatchSave() {
        var student2 = new Student();
        student2.setId(UUID.randomUUID());
        student2.setFullName("Student 2");
        student2.setActive(true);
        student2.setBillingEnabled(true);

        when(billingConfigurationRepository.findByStudioId(studioId)).thenReturn(Optional.of(config));
        when(studentRepository.findByStudioIdAndActiveTrue(studioId)).thenReturn(List.of(activeStudent, student2));
        when(invoiceRepository.findByStudentIdAndBillingMonthAndStudioId(
                any(), eq("2026-08"), eq(studioId))).thenReturn(Optional.empty());

        var request = new GenerateInvoiceRequest();
        request.setMonth("2026-08");

        GenerateInvoiceResponse response = service.generate(request, studioId);

        assertThat(response.getCreated()).isEqualTo(2);
        assertThat(response.getIgnored()).isEqualTo(0);

        verify(invoiceRepository, times(1)).saveAll(any());
    }
}
