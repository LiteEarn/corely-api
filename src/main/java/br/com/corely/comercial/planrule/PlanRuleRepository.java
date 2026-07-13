package br.com.corely.comercial.planrule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRuleRepository extends JpaRepository<PlanRule, UUID> {

    List<PlanRule> findByPlanIdOrderByCreatedAt(UUID planId);

    Optional<PlanRule> findByPlanIdAndId(UUID planId, UUID id);

    boolean existsByPlanIdAndRuleDefinitionId(UUID planId, UUID ruleDefinitionId);
}
