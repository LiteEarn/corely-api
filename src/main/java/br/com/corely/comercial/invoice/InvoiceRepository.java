package br.com.corely.comercial.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
