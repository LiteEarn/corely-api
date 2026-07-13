package br.com.corely.comercial.ruledefinition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleDefinitionRepository extends JpaRepository<RuleDefinition, UUID> {

    List<RuleDefinition> findByActiveTrueOrderByName();

    Optional<RuleDefinition> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    @Override
    @Query("SELECT r FROM RuleDefinition r WHERE r.id = :id")
    Optional<RuleDefinition> findById(@Param("id") UUID id);
}
