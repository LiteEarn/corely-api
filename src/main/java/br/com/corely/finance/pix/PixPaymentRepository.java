package br.com.corely.finance.pix;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository("financePixPaymentRepository")
public interface PixPaymentRepository extends JpaRepository<PixPayment, UUID> {

    @Override
    @Query("SELECT p FROM FinancePixPayment p WHERE p.id = :id")
    @EntityGraph(attributePaths = {"receivable", "receivable.student"})
    Optional<PixPayment> findById(@Param("id") UUID id);

    @Query("""
            SELECT p FROM FinancePixPayment p
            WHERE p.studio.id = :studioId
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    @EntityGraph(attributePaths = {"receivable", "receivable.student"})
    Page<PixPayment> findByStudioId(@Param("studioId") UUID studioId, Pageable pageable);

    @Query("SELECT p FROM FinancePixPayment p WHERE p.txid = :txid")
    @EntityGraph(attributePaths = {"receivable", "receivable.student"})
    Optional<PixPayment> findByTxid(@Param("txid") String txid);

    @Query("SELECT COUNT(p) > 0 FROM FinancePixPayment p WHERE p.receivable.id = :receivableId")
    boolean existsByReceivableId(@Param("receivableId") UUID receivableId);
}