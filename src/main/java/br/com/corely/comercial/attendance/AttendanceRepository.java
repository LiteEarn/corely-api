package br.com.corely.comercial.attendance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository("comercialAttendanceRepository")
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    @Query("SELECT a FROM ComercialAttendance a WHERE a.classSession.id = :classSessionId")
    Page<Attendance> findByClassSessionId(@Param("classSessionId") UUID classSessionId, Pageable pageable);

    @Query("SELECT a FROM ComercialAttendance a WHERE a.booking.id = :bookingId")
    Page<Attendance> findByBookingId(@Param("bookingId") UUID bookingId, Pageable pageable);

    @Query("SELECT a FROM ComercialAttendance a WHERE a.booking.student.id = :studentId")
    Page<Attendance> findByStudentId(@Param("studentId") UUID studentId, Pageable pageable);

    @Query("SELECT a FROM ComercialAttendance a WHERE a.classSession.id = :classSessionId AND a.booking.id = :bookingId")
    Optional<Attendance> findByClassSessionIdAndBookingId(@Param("classSessionId") UUID classSessionId,
                                                           @Param("bookingId") UUID bookingId);

    @Query("SELECT COUNT(a) FROM ComercialAttendance a WHERE a.classSession.id = :classSessionId AND a.status = 'PRESENT'")
    long countPresentByClassSessionId(@Param("classSessionId") UUID classSessionId);

    @Override
    @Query("SELECT a FROM ComercialAttendance a WHERE a.id = :id")
    Optional<Attendance> findById(@Param("id") UUID id);
}
