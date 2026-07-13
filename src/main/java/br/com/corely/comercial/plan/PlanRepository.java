package br.com.corely.comercial.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findByStudioIdAndActiveTrueOrderByName(UUID studioId);
    List<Plan> findByStudioIdOrderByName(UUID studioId);
    long countByStudioIdAndActiveTrue(UUID studioId);
}
