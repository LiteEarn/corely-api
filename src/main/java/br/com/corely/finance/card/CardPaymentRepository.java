package br.com.corely.finance.card;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository("financeCardPaymentRepository")
public interface CardPaymentRepository extends JpaRepository<CardPayment, UUID> {

    @Override
    @Query("SELECT c FROM FinanceCardPayment c WHERE c.id = :id")
    @EntityGraph(attributePaths = {"receivable", "receivable.student"})
    Optional<CardPayment> findById(@Param("id") UUID id);

    @Query("""
            SELECT c FROM FinanceCardPayment c
            WHERE c.studio.id = :studioId
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    @EntityGraph(attributePaths = {"receivable", "receivable.student"})
    Page<CardPayment> findByStudioId(@Param("studioId") UUID studioId, Pageable pageable);

    @Query("SELECT c FROM FinanceCardPayment c WHERE c.transactionId = :transactionId")
    @EntityGraph(attributePaths = {"receivable", "receivable.student"})
    Optional<CardPayment> findByTransactionId(@Param("transactionId") String transactionId);

    @Query("SELECT COUNT(c) > 0 FROM FinanceCardPayment c WHERE c.receivable.id = :receivableId")
    boolean existsByReceivableId(@Param("receivableId") UUID receivableId);
}