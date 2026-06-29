package br.com.corely.classsession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {
    boolean existsByClassGroupIdAndSessionDate(UUID classGroupId, LocalDate sessionDate);
    List<ClassSession> findByClassGroupId(UUID classGroupId);
    List<ClassSession> findBySessionDate(LocalDate sessionDate);
    List<ClassSession> findByStatus(ClassSessionStatus status);
    List<ClassSession> findByInstructorId(UUID instructorId);

    @Query("SELECT cs FROM ClassSession cs WHERE " +
           "(:classGroupId IS NULL OR cs.classGroup.id = :classGroupId) AND " +
           "(:instructorId IS NULL OR cs.instructor.id = :instructorId) AND " +
           "(:status IS NULL OR cs.status = :status) AND " +
           "(:sessionDate IS NULL OR cs.sessionDate = :sessionDate)")
    List<ClassSession> findWithFilters(@Param("classGroupId") UUID classGroupId,
                                       @Param("instructorId") UUID instructorId,
                                       @Param("status") ClassSessionStatus status,
                                       @Param("sessionDate") LocalDate sessionDate);
}
