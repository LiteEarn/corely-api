package br.com.corely.comercial.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findByActiveTrueOrderByName();

    @Override
    @Query("SELECT p FROM Plan p WHERE p.id = :id")
    Optional<Plan> findById(@Param("id") UUID id);
}
