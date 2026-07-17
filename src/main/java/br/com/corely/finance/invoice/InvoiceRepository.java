package br.com.corely.finance.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("financeInvoiceRepository")
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @Query("SELECT i FROM FinanceInvoice i WHERE i.student.id = :studentId")
    List<Invoice> findByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT i FROM FinanceInvoice i WHERE i.status = :status")
    List<Invoice> findByStatus(@Param("status") InvoiceStatus status);

    @Override
    @Query("SELECT i FROM FinanceInvoice i WHERE i.id = :id")
    Optional<Invoice> findById(@Param("id") UUID id);

    @Override
    @Query("SELECT i FROM FinanceInvoice i")
    List<Invoice> findAll();

    @Query("SELECT i FROM FinanceInvoice i WHERE i.student.id = :studentId AND i.billingMonth = :billingMonth AND i.studio.id = :studioId")
    Optional<Invoice> findByStudentIdAndBillingMonthAndStudioId(
            @Param("studentId") UUID studentId,
            @Param("billingMonth") String billingMonth,
            @Param("studioId") UUID studioId);

    @Query("SELECT i FROM FinanceInvoice i WHERE i.studio.id = :studioId AND i.billingMonth = :billingMonth")
    List<Invoice> findByStudioIdAndBillingMonth(
            @Param("studioId") UUID studioId,
            @Param("billingMonth") String billingMonth);

    @Query("SELECT COUNT(i) FROM FinanceInvoice i WHERE i.studio.id = :studioId AND i.status = :status AND i.billingMonth = :billingMonth")
    long countByStudioIdAndStatusAndBillingMonth(
            @Param("studioId") UUID studioId,
            @Param("status") InvoiceStatus status,
            @Param("billingMonth") String billingMonth);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM FinanceInvoice i WHERE i.studio.id = :studioId AND i.status = :status AND i.billingMonth = :billingMonth")
    java.math.BigDecimal sumAmountByStudioIdAndStatusAndBillingMonth(
            @Param("studioId") UUID studioId,
            @Param("status") InvoiceStatus status,
            @Param("billingMonth") String billingMonth);

    @Query("SELECT COUNT(DISTINCT i.student.id) FROM FinanceInvoice i WHERE i.studio.id = :studioId AND i.billingMonth = :billingMonth")
    long countDistinctStudentByStudioIdAndBillingMonth(
            @Param("studioId") UUID studioId,
            @Param("billingMonth") String billingMonth);
}
