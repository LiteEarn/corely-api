package br.com.corely.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeBlockRepository extends JpaRepository<TimeBlock, UUID> {

    @Query("SELECT tb FROM TimeBlock tb WHERE tb.studio.id = :studioId "
            + "AND tb.startDate < :endDate AND tb.endDate > :startDate")
    List<TimeBlock> findActiveBlocks(@Param("studioId") UUID studioId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT tb FROM TimeBlock tb WHERE tb.studio.id = :studioId "
            + "AND tb.instructorId = :instructorId "
            + "AND tb.startDate < :endDate AND tb.endDate > :startDate")
    List<TimeBlock> findBlocksByInstructor(@Param("studioId") UUID studioId,
                                           @Param("instructorId") Long instructorId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT tb FROM TimeBlock tb WHERE tb.studio.id = :studioId "
            + "AND tb.roomId = :roomId "
            + "AND tb.startDate < :endDate AND tb.endDate > :startDate")
    List<TimeBlock> findBlocksByRoom(@Param("studioId") UUID studioId,
                                     @Param("roomId") Long roomId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);
}
