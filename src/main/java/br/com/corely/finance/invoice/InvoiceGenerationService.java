package br.com.corely.finance.invoice;

import br.com.corely.billingconfiguration.BillingConfiguration;
import br.com.corely.billingconfiguration.BillingConfigurationRepository;
import br.com.corely.finance.invoice.dto.DashboardResponse;
import br.com.corely.finance.invoice.dto.GenerateInvoiceRequest;
import br.com.corely.finance.invoice.dto.GenerateInvoiceResponse;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service("financeInvoiceGenerationService")
@RequiredArgsConstructor
public class InvoiceGenerationService {

    private final InvoiceRepository invoiceRepository;
    private final StudentRepository studentRepository;
    private final BillingConfigurationRepository billingConfigurationRepository;

    @Transactional
    public GenerateInvoiceResponse generate(GenerateInvoiceRequest request, UUID studioId) {
        long start = System.currentTimeMillis();

        var config = billingConfigurationRepository.findByStudioId(studioId)
                .orElseThrow(() -> new ResourceNotFoundException("BillingConfiguration not found for this studio"));

        if (!Boolean.TRUE.equals(config.getActive())) {
            throw new IllegalStateException("Billing configuration is inactive");
        }

        String billingMonth = request.getMonth();
        int created = 0;
        int ignored = 0;

        List<Student> students = studentRepository.findByStudioIdAndActiveTrue(studioId);

        List<Invoice> toSave = new ArrayList<>();

        for (var student : students) {
            if (!Boolean.TRUE.equals(student.getBillingEnabled())) {
                ignored++;
                continue;
            }

            var existing = invoiceRepository.findByStudentIdAndBillingMonthAndStudioId(
                    student.getId(), billingMonth, studioId);
            if (existing.isPresent()) {
                ignored++;
                continue;
            }

            var invoice = new Invoice();
            invoice.setStudio(config.getStudio());
            invoice.setStudent(student);
            invoice.setBillingMonth(billingMonth);
            invoice.setAmount(config.getDefaultAmount());
            invoice.setStatus(InvoiceStatus.PENDING);

            int dueDay = clampDay(config.getDueDay(), billingMonth);
            invoice.setDueDate(LocalDate.of(
                    Integer.parseInt(billingMonth.substring(0, 4)),
                    Integer.parseInt(billingMonth.substring(5, 7)),
                    dueDay));

            toSave.add(invoice);
            created++;
        }

        if (!toSave.isEmpty()) {
            invoiceRepository.saveAll(toSave);
        }

        long executionTime = System.currentTimeMillis() - start;
        return new GenerateInvoiceResponse(created, ignored, executionTime);
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(String billingMonth, UUID studioId) {
        var config = billingConfigurationRepository.findByStudioId(studioId);

        long pending = invoiceRepository.countByStudioIdAndStatusAndBillingMonth(studioId, InvoiceStatus.PENDING, billingMonth);
        long paid = invoiceRepository.countByStudioIdAndStatusAndBillingMonth(studioId, InvoiceStatus.PAID, billingMonth);
        long overdue = invoiceRepository.countByStudioIdAndStatusAndBillingMonth(studioId, InvoiceStatus.OVERDUE, billingMonth);
        long cancelled = invoiceRepository.countByStudioIdAndStatusAndBillingMonth(studioId, InvoiceStatus.CANCELLED, billingMonth);

        BigDecimal expectedRevenue = invoiceRepository.sumAmountByStudioIdAndStatusAndBillingMonth(studioId, InvoiceStatus.PENDING, billingMonth)
                .add(invoiceRepository.sumAmountByStudioIdAndStatusAndBillingMonth(studioId, InvoiceStatus.PAID, billingMonth))
                .add(invoiceRepository.sumAmountByStudioIdAndStatusAndBillingMonth(studioId, InvoiceStatus.OVERDUE, billingMonth));
        BigDecimal receivedRevenue = invoiceRepository.sumAmountByStudioIdAndStatusAndBillingMonth(studioId, InvoiceStatus.PAID, billingMonth);
        BigDecimal pendingRevenue = invoiceRepository.sumAmountByStudioIdAndStatusAndBillingMonth(studioId, InvoiceStatus.PENDING, billingMonth)
                .add(invoiceRepository.sumAmountByStudioIdAndStatusAndBillingMonth(studioId, InvoiceStatus.OVERDUE, billingMonth));
        long totalBilledStudents = invoiceRepository.countDistinctStudentByStudioIdAndBillingMonth(studioId, billingMonth);

        return new DashboardResponse(pending, paid, overdue, cancelled,
                expectedRevenue, receivedRevenue, pendingRevenue, totalBilledStudents);
    }

    private static int clampDay(int day, String billingMonth) {
        int year = Integer.parseInt(billingMonth.substring(0, 4));
        int month = Integer.parseInt(billingMonth.substring(5, 7));
        LocalDate lastDay = LocalDate.of(year, month, 1).withDayOfMonth(
                LocalDate.of(year, month, 1).lengthOfMonth());
        return Math.min(day, lastDay.getDayOfMonth());
    }
}
