package br.com.corely.makeup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MakeupRequestRepository extends JpaRepository<MakeupRequest, UUID> {

    Optional<MakeupRequest> findByAttendanceId(UUID attendanceId);

    boolean existsByAttendanceId(UUID attendanceId);

    List<MakeupRequest> findByStatus(MakeupRequestStatus status);

    @Query("SELECT mr FROM MakeupRequest mr " +
           "JOIN FETCH mr.attendance a " +
           "JOIN FETCH a.enrollment e " +
           "JOIN FETCH e.student s " +
           "JOIN FETCH e.classGroup cg " +
           "WHERE e.studio.id = :studioId AND mr.status = :status")
    List<MakeupRequest> findByStudioIdAndStatus(@Param("studioId") UUID studioId, @Param("status") MakeupRequestStatus status);

    @Query("SELECT COUNT(mr) FROM MakeupRequest mr " +
           "JOIN mr.attendance a " +
           "JOIN a.enrollment e " +
           "WHERE e.studio.id = :studioId AND mr.status = :status")
    long countByStudioIdAndStatus(@Param("studioId") UUID studioId, @Param("status") MakeupRequestStatus status);
}
