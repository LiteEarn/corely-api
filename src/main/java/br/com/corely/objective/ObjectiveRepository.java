package br.com.corely.objective;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ObjectiveRepository extends JpaRepository<Objective, UUID> {

    List<Objective> findByStudentId(UUID studentId);

    List<Objective> findByStudentIdAndStatus(UUID studentId, ObjectiveStatus status);

    @Query("SELECT o FROM Objective o WHERE " +
           "(:studentId IS NULL OR o.student.id = :studentId) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:search IS NULL OR LOWER(o.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    List<Objective> findAllWithFilters(
            @Param("studentId") UUID studentId,
            @Param("status") ObjectiveStatus status,
            @Param("search") String search
    );
}
