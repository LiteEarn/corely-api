package br.com.corely.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    List<Evaluation> findByStudentId(UUID studentId);

    List<Evaluation> findByEvaluationDateBetween(LocalDate startDate, LocalDate endDate);

    List<Evaluation> findByStudentIdAndEvaluationDateBetween(UUID studentId, LocalDate startDate, LocalDate endDate);
}
