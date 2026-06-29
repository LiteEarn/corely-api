package br.com.corely.makeup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MakeupRequestRepository extends JpaRepository<MakeupRequest, UUID> {

    Optional<MakeupRequest> findByAttendanceId(UUID attendanceId);

    boolean existsByAttendanceId(UUID attendanceId);

    List<MakeupRequest> findByStatus(MakeupRequestStatus status);
}
