package br.com.corely.finance.movement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository("receivableMovementRepository")
public interface ReceivableMovementRepository extends JpaRepository<ReceivableMovement, UUID> {

    @Query("""
            SELECT m FROM ReceivableMovement m
            WHERE m.studio.id = :studioId
              AND m.receivable.id = :receivableId
            ORDER BY m.occurredAt DESC, m.id DESC
            """)
    Page<ReceivableMovement> findByReceivableId(@Param("studioId") UUID studioId,
                                                @Param("receivableId") UUID receivableId,
                                                Pageable pageable);
}
