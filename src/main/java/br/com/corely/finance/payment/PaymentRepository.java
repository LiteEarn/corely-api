package br.com.corely.finance.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository("financePaymentRepository")
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Override
    @Query("SELECT p FROM FinancePayment p WHERE p.id = :id")
    @EntityGraph(attributePaths = {"receivable", "receivable.student"})
    Optional<Payment> findById(@Param("id") UUID id);

    @Query("""
            SELECT p FROM FinancePayment p
            WHERE p.studio.id = :studioId
            ORDER BY p.paymentDate DESC, p.id DESC
            """)
    @EntityGraph(attributePaths = {"receivable", "receivable.student"})
    Page<Payment> findByStudioId(@Param("studioId") UUID studioId, Pageable pageable);

    @Query("""
            SELECT p FROM FinancePayment p
            WHERE p.studio.id = :studioId AND p.paymentMethod = :paymentMethod
            ORDER BY p.paymentDate DESC, p.id DESC
            """)
    @EntityGraph(attributePaths = {"receivable", "receivable.student"})
    Page<Payment> findByStudioIdAndPaymentMethod(@Param("studioId") UUID studioId,
                                                 @Param("paymentMethod") PaymentMethod paymentMethod,
                                                 Pageable pageable);

    @Query("""
            SELECT p FROM FinancePayment p
            WHERE p.studio.id = :studioId AND p.refundedAt IS NOT NULL
            ORDER BY p.refundedAt DESC, p.id DESC
            """)
    @EntityGraph(attributePaths = {"receivable", "receivable.student"})
    Page<Payment> findRefundedByStudioId(@Param("studioId") UUID studioId, Pageable pageable);

    @Query("SELECT COUNT(p) > 0 FROM FinancePayment p WHERE p.receivable.id = :receivableId")
    boolean existsByReceivableId(@Param("receivableId") UUID receivableId);
}