package br.com.corely.comercial.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findAllByOrderByCreatedAtDesc();

    Optional<Invoice> findByStudentPlanIdAndReferenceMonth(UUID studentPlanId, String referenceMonth);

    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);

    @Query("SELECT i FROM Invoice i WHERE i.studentPlan.id = :studentPlanId AND i.status = :status ORDER BY i.dueDate ASC")
    List<Invoice> findByStudentPlanIdAndStatusOrderByDueDateAsc(
            @Param("studentPlanId") UUID studentPlanId,
            @Param("status") InvoiceStatus status);
}
