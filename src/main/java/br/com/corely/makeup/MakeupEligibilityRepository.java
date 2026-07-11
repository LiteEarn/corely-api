package br.com.corely.makeup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MakeupEligibilityRepository extends JpaRepository<MakeupEligibility, UUID> {
}
