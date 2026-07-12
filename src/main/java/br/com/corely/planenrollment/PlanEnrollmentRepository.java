package br.com.corely.planenrollment;

import br.com.corely.planenrollment.dto.PlanEnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanEnrollmentRepository extends JpaRepository<PlanEnrollment, UUID> {

    Optional<PlanEnrollment> findByStudentIdAndStatus(UUID studentId, PlanEnrollmentStatus status);

    List<PlanEnrollment> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    boolean existsByStudentIdAndStatus(UUID studentId, PlanEnrollmentStatus status);

    boolean existsByPlanIdAndStatus(UUID planId, PlanEnrollmentStatus status);

    List<PlanEnrollment> findByPlanId(UUID planId);
}
