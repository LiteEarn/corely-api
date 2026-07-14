package br.com.corely.comercial.scheduleslot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, UUID> {

    List<ScheduleSlot> findByScheduleIdOrderByDayOfWeekAscStartTimeAsc(UUID scheduleId);

    @Query("SELECT COUNT(s) > 0 FROM ScheduleSlot s WHERE s.schedule.id = :scheduleId " +
           "AND s.dayOfWeek = :dayOfWeek " +
           "AND s.startTime < :endTime AND s.endTime > :startTime " +
           "AND (:excludeId IS NULL OR s.id <> :excludeId)")
    boolean existsOverlappingSlot(@Param("scheduleId") UUID scheduleId,
                                  @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                  @Param("startTime") LocalTime startTime,
                                  @Param("endTime") LocalTime endTime,
                                  @Param("excludeId") UUID excludeId);

    @Override
    @Query("SELECT s FROM ScheduleSlot s WHERE s.id = :id")
    Optional<ScheduleSlot> findById(@Param("id") UUID id);
}
