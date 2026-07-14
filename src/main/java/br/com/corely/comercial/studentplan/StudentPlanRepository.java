package br.com.corely.comercial.studentplan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentPlanRepository extends JpaRepository<StudentPlan, UUID> {

    Optional<StudentPlan> findByStudentIdAndStatus(UUID studentId, StudentPlanStatus status);

    List<StudentPlan> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    boolean existsByStudentIdAndStatus(UUID studentId, StudentPlanStatus status);

    @Query("SELECT sp FROM StudentPlan sp WHERE sp.status = :status")
    List<StudentPlan> findByStatus(@Param("status") StudentPlanStatus status);
}
