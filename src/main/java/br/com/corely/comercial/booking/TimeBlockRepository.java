package br.com.corely.comercial.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("comercialTimeBlockRepository")
public interface TimeBlockRepository extends JpaRepository<TimeBlock, UUID> {

    @Override
    @Query("SELECT t FROM ComercialTimeBlock t WHERE t.id = :id")
    Optional<TimeBlock> findById(@Param("id") UUID id);

    @Query("SELECT t FROM ComercialTimeBlock t WHERE t.active = true " +
           "AND (:instructorId IS NULL OR t.instructor.id = :instructorId) " +
           "AND (:roomId IS NULL OR t.roomId = :roomId) " +
           "AND t.startDateTime < :endDateTime AND t.endDateTime > :startDateTime")
    List<TimeBlock> findActiveOverlapping(@Param("instructorId") UUID instructorId,
                                          @Param("roomId") Long roomId,
                                          @Param("startDateTime") LocalDateTime startDateTime,
                                          @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT t FROM ComercialTimeBlock t WHERE t.active = true " +
           "AND (:instructorId IS NULL OR t.instructor.id = :instructorId) " +
           "AND (:roomId IS NULL OR t.roomId = :roomId) " +
           "AND (:startDate IS NULL OR t.startDateTime >= :startDate) " +
           "AND (:endDate IS NULL OR t.endDateTime <= :endDate)")
    Page<TimeBlock> findByFilters(@Param("instructorId") UUID instructorId,
                                  @Param("roomId") Long roomId,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  Pageable pageable);
}
