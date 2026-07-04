package br.com.corely.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    List<Attendance> findByClassSessionId(UUID classSessionId);

    List<Attendance> findByEnrollmentId(UUID enrollmentId);

    Optional<Attendance> findByClassSessionIdAndEnrollmentId(UUID classSessionId, UUID enrollmentId);

    boolean existsByClassSessionIdAndEnrollmentId(UUID classSessionId, UUID enrollmentId);

    @Query("SELECT a FROM Attendance a " +
           "JOIN FETCH a.enrollment e " +
           "JOIN FETCH e.student s " +
           "JOIN a.classSession cs " +
           "WHERE cs.classGroup.id = :classGroupId " +
           "AND cs.sessionDate = :date " +
           "AND e.studio.id = :studioId")
    List<Attendance> findByClassGroupIdAndDate(
            @Param("classGroupId") UUID classGroupId,
            @Param("date") LocalDate date,
            @Param("studioId") UUID studioId
    );

    @Query("SELECT COUNT(a) FROM Attendance a " +
           "JOIN a.enrollment e " +
           "JOIN a.classSession cs " +
           "WHERE e.studio.id = :studioId " +
           "AND cs.sessionDate BETWEEN :startDate AND :endDate " +
           "AND a.status = 'PRESENT'")
    long countByStudioIdAndSessionDateBetweenAndPresent(
            @Param("studioId") UUID studioId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(a) FROM Attendance a " +
           "JOIN a.enrollment e " +
           "JOIN a.classSession cs " +
           "WHERE e.studio.id = :studioId " +
           "AND cs.sessionDate = :date " +
           "AND a.status = 'PRESENT'")
    long countPresentByStudioIdAndSessionDate(@Param("studioId") UUID studioId, @Param("date") LocalDate date);
}
