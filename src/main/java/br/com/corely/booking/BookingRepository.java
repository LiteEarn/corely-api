package br.com.corely.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("SELECT b FROM Booking b WHERE b.studio.id = :studioId "
            + "AND b.startDateTime >= :startDate AND b.endDateTime <= :endDate "
            + "AND b.active = true "
            + "ORDER BY b.startDateTime ASC")
    List<Booking> findByStudioAndDateRange(@Param("studioId") UUID studioId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT b FROM Booking b WHERE b.student.id = :studentId "
            + "AND b.startDateTime < :endDate AND b.endDateTime > :startDate "
            + "AND b.active = true AND b.status <> 'CANCELLED' AND b.status <> 'NO_SHOW'")
    List<Booking> findConflictingByStudent(@Param("studentId") UUID studentId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT b FROM Booking b WHERE b.instructor.id = :instructorId "
            + "AND b.startDateTime < :endDate AND b.endDateTime > :startDate "
            + "AND b.active = true AND b.status <> 'CANCELLED' AND b.status <> 'NO_SHOW'")
    List<Booking> findConflictingByInstructor(@Param("instructorId") UUID instructorId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId "
            + "AND b.startDateTime < :endDate AND b.endDateTime > :startDate "
            + "AND b.active = true AND b.status <> 'CANCELLED' AND b.status <> 'NO_SHOW'")
    List<Booking> findConflictingByRoom(@Param("roomId") Long roomId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.roomId = :roomId "
            + "AND b.startDateTime < :endDate AND b.endDateTime > :startDate "
            + "AND b.active = true AND b.status <> 'CANCELLED' AND b.status <> 'NO_SHOW'")
    long countConflictingByRoom(@Param("roomId") Long roomId,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    long countByStudioIdAndStatusAndStartDateTimeBetween(UUID studioId, BookingStatus status,
                                                         LocalDateTime start, LocalDateTime end);

    long countByStudioIdAndStartDateTimeBetween(UUID studioId,
                                                LocalDateTime start, LocalDateTime end);

    long countByStudioIdAndStartDateTimeBetweenAndActiveTrue(UUID studioId,
                                                             LocalDateTime start, LocalDateTime end);
}
