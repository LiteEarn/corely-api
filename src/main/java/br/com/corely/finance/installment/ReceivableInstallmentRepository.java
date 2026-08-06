package br.com.corely.finance.installment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository("receivableInstallmentRepository")
public interface ReceivableInstallmentRepository extends JpaRepository<ReceivableInstallment, UUID> {

    @Override
    @Query("SELECT i FROM ReceivableInstallment i WHERE i.id = :id")
    @EntityGraph(attributePaths = {"studentPlan", "studentPlan.student", "receivable"})
    Optional<ReceivableInstallment> findById(@Param("id") UUID id);

    @Query("""
            SELECT i FROM ReceivableInstallment i
            LEFT JOIN FETCH i.studentPlan sp
            LEFT JOIN FETCH sp.student
            WHERE i.studio.id = :studioId
              AND (:status IS NULL OR i.status = :status)
              AND (:studentPlanId IS NULL OR i.studentPlan.id = :studentPlanId)
              AND (:dueDateFrom IS NULL OR i.dueDate >= :dueDateFrom)
              AND (:dueDateTo IS NULL OR i.dueDate <= :dueDateTo)
            ORDER BY i.dueDate ASC
            """)
    Page<ReceivableInstallment> findByFilters(@Param("studioId") UUID studioId,
                                              @Param("status") InstallmentStatus status,
                                              @Param("studentPlanId") UUID studentPlanId,
                                              @Param("dueDateFrom") LocalDate dueDateFrom,
                                              @Param("dueDateTo") LocalDate dueDateTo,
                                              Pageable pageable);
}
