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
    Optional<Attendance> findByStudentIdAndClassGroupIdAndAttendanceDate(
            UUID studentId, UUID classGroupId, LocalDate attendanceDate
    );

    List<Attendance> findByClassGroupId(UUID classGroupId);

    List<Attendance> findByClassGroupIdAndStudentActiveTrue(UUID classGroupId);

    List<Attendance> findByClassGroupIdAndAttendanceDate(
            UUID classGroupId, LocalDate attendanceDate
    );

    List<Attendance> findByClassGroupIdAndAttendanceDateAndStudentActiveTrue(
            UUID classGroupId, LocalDate attendanceDate
    );

    long countByStudioIdAndAttendanceDateBetweenAndPresentTrue(
            UUID studioId, LocalDate startDate, LocalDate endDate
    );
}
