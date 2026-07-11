package br.com.corely.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findByActiveTrue();
    List<Plan> findByStudioIdAndActiveTrue(UUID studioId);
    List<Plan> findByStudioId(UUID studioId);
    long countByActiveTrueAndId(UUID id);
}
