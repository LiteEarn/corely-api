package br.com.corely.classsession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID>, JpaSpecificationExecutor<ClassSession> {
    boolean existsByClassGroupIdAndSessionDate(UUID classGroupId, LocalDate sessionDate);
    List<ClassSession> findByClassGroupId(UUID classGroupId);
    List<ClassSession> findBySessionDate(LocalDate sessionDate);
    List<ClassSession> findByStatus(ClassSessionStatus status);
    List<ClassSession> findByInstructorId(UUID instructorId);
    List<ClassSession> findByClassGroupIdAndSessionDateGreaterThanEqualAndStatus(UUID classGroupId, LocalDate sessionDate, ClassSessionStatus status);

    Optional<ClassSession> findFirstByClassGroupIdAndSessionDateAndStatusOrderByStartTime(
            UUID classGroupId, LocalDate sessionDate, ClassSessionStatus status);

    @Query("SELECT COUNT(cs) FROM ClassSession cs JOIN cs.classGroup cg WHERE cg.studio.id = :studioId AND cs.sessionDate = :date")
    long countByStudioIdAndSessionDate(@Param("studioId") UUID studioId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(cs) FROM ClassSession cs JOIN cs.classGroup cg WHERE cg.studio.id = :studioId AND cs.status = :status")
    long countByStudioIdAndStatus(@Param("studioId") UUID studioId, @Param("status") ClassSessionStatus status);

    @Query("SELECT cs FROM ClassSession cs JOIN FETCH cs.classGroup cg JOIN FETCH cs.instructor i WHERE cg.studio.id = :studioId AND cs.sessionDate = :date ORDER BY cs.startTime")
    List<ClassSession> findTodaySessionsByStudio(@Param("studioId") UUID studioId, @Param("date") LocalDate date);
}
