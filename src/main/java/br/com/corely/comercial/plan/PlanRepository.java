package br.com.corely.comercial.plan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    Page<Plan> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Plan> findByActive(Boolean active, Pageable pageable);

    Page<Plan> findByNameContainingIgnoreCaseAndActive(String name, Boolean active, Pageable pageable);

    @Override
    @Query("SELECT p FROM Plan p WHERE p.id = :id")
    Optional<Plan> findById(@Param("id") UUID id);
}
