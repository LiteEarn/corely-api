package br.com.corely.finance.cashflow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository("financeCashFlowEntryRepository")
public interface CashFlowEntryRepository extends JpaRepository<CashFlowEntry, UUID> {

    @Override
    @Query("SELECT e FROM FinanceCashFlowEntry e WHERE e.id = :id")
    @EntityGraph(attributePaths = {"payment"})
    Optional<CashFlowEntry> findById(@Param("id") UUID id);

    @Query("""
            SELECT e FROM FinanceCashFlowEntry e
            WHERE e.studio.id = :studioId
              AND (:entryType IS NULL OR e.entryType = :entryType)
              AND (:dateFrom IS NULL OR e.entryDate >= :dateFrom)
              AND (:dateTo IS NULL OR e.entryDate <= :dateTo)
            ORDER BY e.entryDate DESC, e.id DESC
            """)
    @EntityGraph(attributePaths = {"payment"})
    Page<CashFlowEntry> findByFilters(@Param("studioId") UUID studioId,
                                      @Param("entryType") CashFlowEntryType entryType,
                                      @Param("dateFrom") LocalDate dateFrom,
                                      @Param("dateTo") LocalDate dateTo,
                                      Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM FinanceCashFlowEntry e
            WHERE e.studio.id = :studioId
              AND e.entryType = :entryType
              AND (:dateFrom IS NULL OR e.entryDate >= :dateFrom)
              AND (:dateTo IS NULL OR e.entryDate <= :dateTo)
            """)
    BigDecimal sumAmountByStudioIdAndTypeAndPeriod(@Param("studioId") UUID studioId,
                                                   @Param("entryType") CashFlowEntryType entryType,
                                                   @Param("dateFrom") LocalDate dateFrom,
                                                   @Param("dateTo") LocalDate dateTo);
}
