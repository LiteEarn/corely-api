package br.com.corely.finance.receivable;

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

@Repository("receivableRepository")
public interface ReceivableRepository extends JpaRepository<Receivable, UUID> {

    @Override
    @Query("SELECT r FROM Receivable r WHERE r.id = :id")
    @EntityGraph(attributePaths = "student")
    Optional<Receivable> findById(@Param("id") UUID id);

    @Query("""
            SELECT r FROM Receivable r
            WHERE r.studio.id = :studioId
              AND (:status IS NULL OR r.status = :status)
              AND (:studentId IS NULL OR r.student.id = :studentId)
              AND (:dueDateFrom IS NULL OR r.dueDate >= :dueDateFrom)
              AND (:dueDateTo IS NULL OR r.dueDate <= :dueDateTo)
            ORDER BY r.dueDate ASC
            """)
    @EntityGraph(attributePaths = "student")
    Page<Receivable> findByFilters(@Param("studioId") UUID studioId,
                                   @Param("status") ReceivableStatus status,
                                   @Param("studentId") UUID studentId,
                                   @Param("dueDateFrom") LocalDate dueDateFrom,
                                   @Param("dueDateTo") LocalDate dueDateTo,
                                   Pageable pageable);

    @Query("""
            SELECT r FROM Receivable r
            WHERE r.studio.id = :studioId
              AND (:status IS NULL OR r.status = :status)
              AND (:overdue IS NULL OR (:overdue = true AND r.dueDate < :today)
                    OR (:overdue = false AND r.dueDate >= :today))
              AND (:studentId IS NULL OR r.student.id = :studentId)
              AND (:dueDateFrom IS NULL OR r.dueDate >= :dueDateFrom)
              AND (:dueDateTo IS NULL OR r.dueDate <= :dueDateTo)
            ORDER BY r.dueDate ASC
            """)
    @EntityGraph(attributePaths = "student")
    Page<Receivable> findBySituation(@Param("studioId") UUID studioId,
                                     @Param("status") ReceivableStatus status,
                                     @Param("overdue") Boolean overdue,
                                     @Param("studentId") UUID studentId,
                                     @Param("dueDateFrom") LocalDate dueDateFrom,
                                     @Param("dueDateTo") LocalDate dueDateTo,
                                     @Param("today") LocalDate today,
                                     Pageable pageable);
}
