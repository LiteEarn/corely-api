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
}
