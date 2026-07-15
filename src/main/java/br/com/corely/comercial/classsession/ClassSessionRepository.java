package br.com.corely.comercial.classsession;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository("comercialClassSessionRepository")
public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {

    @Query("SELECT COUNT(c) > 0 FROM ComercialClassSession c WHERE c.scheduleSlot.id = :scheduleSlotId " +
           "AND c.sessionDate = :sessionDate AND (:excludeId IS NULL OR c.id <> :excludeId)")
    boolean existsDuplicate(@Param("scheduleSlotId") UUID scheduleSlotId,
                            @Param("sessionDate") LocalDate sessionDate,
                            @Param("excludeId") UUID excludeId);

    @Query("SELECT c FROM ComercialClassSession c WHERE c.scheduleSlot.id = :scheduleSlotId")
    Page<ClassSession> findByScheduleSlotId(@Param("scheduleSlotId") UUID scheduleSlotId, Pageable pageable);

    @Query("SELECT c FROM ComercialClassSession c WHERE c.status = :status")
    Page<ClassSession> findByStatus(@Param("status") SessionStatus status, Pageable pageable);

    @Override
    @Query("SELECT c FROM ComercialClassSession c WHERE c.id = :id")
    Optional<ClassSession> findById(@Param("id") UUID id);
}
