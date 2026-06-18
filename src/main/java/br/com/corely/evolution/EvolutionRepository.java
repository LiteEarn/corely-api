package br.com.corely.evolution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EvolutionRepository extends JpaRepository<Evolution, UUID> {

    List<Evolution> findByStudentId(UUID studentId);

    List<Evolution> findByObjectiveId(UUID objectiveId);

    List<Evolution> findByEvolutionDateBetween(LocalDate startDate, LocalDate endDate);

    List<Evolution> findByStudentIdAndEvolutionDateBetween(UUID studentId, LocalDate startDate, LocalDate endDate);

    List<Evolution> findByObjectiveIdAndEvolutionDateBetween(UUID objectiveId, LocalDate startDate, LocalDate endDate);

    long countByStudioIdAndEvolutionDateBetween(UUID studioId, LocalDate startDate, LocalDate endDate);

    List<Evolution> findByStudioIdOrderByCreatedAtDesc(UUID studioId);
}
