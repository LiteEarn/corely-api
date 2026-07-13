package br.com.corely.comercial.delinquencypolicy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DelinquencyPolicyRepository extends JpaRepository<DelinquencyPolicy, UUID> {

    @Query("SELECT dp FROM DelinquencyPolicy dp WHERE dp.studio.id = :studioId")
    Optional<DelinquencyPolicy> findByStudioId(@Param("studioId") UUID studioId);
}
