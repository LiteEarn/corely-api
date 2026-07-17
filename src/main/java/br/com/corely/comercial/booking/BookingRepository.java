package br.com.corely.comercial.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
