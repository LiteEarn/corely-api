package br.com.corely.comercial.waitlist;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("comercialWaitListRepository")
public interface WaitListRepository extends JpaRepository<WaitList, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM ComercialWaitList w WHERE w.classSession.id = :classSessionId AND w.status = 'WAITING' AND w.active = true ORDER BY w.position ASC")
    List<WaitList> findWaitingByClassSessionIdWithLock(@Param("classSessionId") UUID classSessionId);

    @Query("SELECT COALESCE(MAX(w.position), 0) FROM ComercialWaitList w WHERE w.classSession.id = :classSessionId")
    Integer findMaxPositionByClassSessionId(@Param("classSessionId") UUID classSessionId);

    @Query("SELECT COUNT(w) > 0 FROM ComercialWaitList w WHERE w.classSession.id = :classSessionId AND w.student.id = :studentId AND w.active = true AND w.status = 'WAITING'")
    boolean existsActiveWaiting(@Param("classSessionId") UUID classSessionId, @Param("studentId") UUID studentId);

    @Override
    @Query("SELECT w FROM ComercialWaitList w WHERE w.id = :id")
    Optional<WaitList> findById(@Param("id") UUID id);

    @Query("SELECT w FROM ComercialWaitList w WHERE w.classSession.id = :classSessionId AND w.active = true")
    Page<WaitList> findActiveByClassSessionId(@Param("classSessionId") UUID classSessionId, Pageable pageable);
}
