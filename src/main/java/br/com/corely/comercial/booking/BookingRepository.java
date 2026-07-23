package br.com.corely.comercial.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("comercialBookingRepository")
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("SELECT COUNT(b) > 0 FROM ComercialBooking b WHERE b.classSession.id = :classSessionId AND b.student.id = :studentId AND b.active = true AND b.status = 'CONFIRMED'")
    boolean existsByClassSessionIdAndStudentId(@Param("classSessionId") UUID classSessionId,
                                                @Param("studentId") UUID studentId);

    @Query("SELECT b FROM ComercialBooking b WHERE b.classSession.id = :classSessionId")
    Page<Booking> findByClassSessionId(@Param("classSessionId") UUID classSessionId, Pageable pageable);

    @Query("SELECT b FROM ComercialBooking b WHERE b.student.id = :studentId")
    Page<Booking> findByStudentId(@Param("studentId") UUID studentId, Pageable pageable);

    @Query("SELECT b FROM ComercialBooking b WHERE b.status = :status")
    Page<Booking> findByStatus(@Param("status") BookingStatus status, Pageable pageable);

    @Override
    @Query("SELECT b FROM ComercialBooking b WHERE b.id = :id")
    Optional<Booking> findById(@Param("id") UUID id);

    @Query("SELECT COUNT(b) FROM ComercialBooking b WHERE b.classSession.id = :classSessionId AND b.status = 'CONFIRMED'")
    long countConfirmedByClassSessionId(@Param("classSessionId") UUID classSessionId);

    @Query("SELECT b.classSession.id, COUNT(b) FROM ComercialBooking b WHERE b.classSession.id IN :sessionIds AND b.status = 'CONFIRMED' GROUP BY b.classSession.id")
    List<Object[]> countConfirmedBySessionIds(@Param("sessionIds") List<UUID> sessionIds);

    @Query("SELECT COUNT(b) FROM ComercialBooking b WHERE b.classSession.id IN :sessionIds AND b.status = 'CONFIRMED'")
    long countConfirmedBySessionIdList(@Param("sessionIds") List<UUID> sessionIds);

    @Query("SELECT b FROM ComercialBooking b JOIN b.classSession cs " +
           "WHERE cs.sessionDate BETWEEN :startDate AND :endDate " +
           "AND (:instructorId IS NULL OR EXISTS (SELECT 1 FROM ScheduleSlot ss WHERE ss.id = cs.scheduleSlot.id AND ss.instructor.id = :instructorId)) " +
           "AND (:roomId IS NULL OR EXISTS (SELECT 1 FROM ScheduleSlot ss WHERE ss.id = cs.scheduleSlot.id AND ss.roomId = :roomId)) " +
           "AND (:studentId IS NULL OR b.student.id = :studentId)")
    Page<Booking> findByAgenda(@Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate,
                               @Param("instructorId") UUID instructorId,
                               @Param("roomId") Long roomId,
                               @Param("studentId") UUID studentId,
                               Pageable pageable);

    @Query("SELECT b FROM ComercialBooking b JOIN b.classSession cs " +
           "WHERE b.status = 'CONFIRMED' AND b.active = true " +
           "AND cs.sessionDate = :sessionDate " +
           "AND cs.startTime < :endTime AND cs.endTime > :startTime " +
           "AND (:instructorId IS NULL OR EXISTS (SELECT 1 FROM ScheduleSlot ss WHERE ss.id = cs.scheduleSlot.id AND ss.instructor.id = :instructorId)) " +
           "AND (:roomId IS NULL OR EXISTS (SELECT 1 FROM ScheduleSlot ss WHERE ss.id = cs.scheduleSlot.id AND ss.roomId = :roomId)) " +
           "AND b.id <> :excludeBookingId")
    List<Booking> findConflictingBookings(@Param("instructorId") UUID instructorId,
                                          @Param("roomId") Long roomId,
                                          @Param("sessionDate") LocalDate sessionDate,
                                          @Param("startTime") LocalTime startTime,
                                          @Param("endTime") LocalTime endTime,
                                          @Param("excludeBookingId") UUID excludeBookingId);

    @Query("SELECT b FROM ComercialBooking b JOIN b.classSession cs " +
           "WHERE b.status = 'CONFIRMED' AND b.active = true " +
           "AND b.student.id = :studentId " +
           "AND cs.sessionDate = :sessionDate " +
           "AND cs.startTime < :endTime AND cs.endTime > :startTime " +
           "AND b.id <> :excludeBookingId")
    List<Booking> findConflictingBookingsForStudent(@Param("studentId") UUID studentId,
                                                    @Param("sessionDate") LocalDate sessionDate,
                                                    @Param("startTime") LocalTime startTime,
                                                    @Param("endTime") LocalTime endTime,
                                                    @Param("excludeBookingId") UUID excludeBookingId);

    @Query("SELECT COUNT(b) FROM ComercialBooking b JOIN b.classSession cs WHERE cs.sessionDate = :date")
    long countBySessionDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(b) FROM ComercialBooking b JOIN b.classSession cs WHERE cs.sessionDate = :date AND b.status = :status")
    long countBySessionDateAndStatus(@Param("date") LocalDate date, @Param("status") BookingStatus status);

    @Query("SELECT b FROM ComercialBooking b WHERE b.classSession.id = :classSessionId AND b.status = :status")
    List<Booking> findByClassSessionIdAndStatus(@Param("classSessionId") UUID classSessionId,
                                                @Param("status") BookingStatus status);
}
