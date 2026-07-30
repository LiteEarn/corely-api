package br.com.corely.financialdashboard;

import br.com.corely.comercial.invoice.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface RevenueDashboardRepository extends JpaRepository<Invoice, UUID> {

    @Query("SELECT i.status, COUNT(i), COALESCE(SUM(i.amount), 0) " +
           "FROM Invoice i WHERE i.referenceMonth = :referenceMonth " +
           "GROUP BY i.status")
    List<Object[]> countAndSumInvoicesByStatusForMonth(@Param("referenceMonth") String referenceMonth);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.referenceMonth = :referenceMonth")
    BigDecimal totalInvoicedForMonth(@Param("referenceMonth") String referenceMonth);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.referenceMonth = :referenceMonth")
    long countInvoicesForMonth(@Param("referenceMonth") String referenceMonth);
}
