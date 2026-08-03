package br.com.corely.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    @Override
    @Query("SELECT e FROM Evaluation e WHERE e.id = :id")
    Optional<Evaluation> findById(@Param("id") UUID id);

    List<Evaluation> findByStudentId(UUID studentId);

    List<Evaluation> findByEvaluationDateBetween(LocalDate startDate, LocalDate endDate);

    List<Evaluation> findByStudentIdAndEvaluationDateBetween(UUID studentId, LocalDate startDate, LocalDate endDate);

    long countByStudioIdAndEvaluationDateBetween(UUID studioId, LocalDate startDate, LocalDate endDate);

    List<Evaluation> findByStudioIdOrderByCreatedAtDesc(UUID studioId);
}
