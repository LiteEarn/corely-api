package br.com.corely.classsession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {
    boolean existsByClassGroupIdAndSessionDate(UUID classGroupId, LocalDate sessionDate);
    List<ClassSession> findByClassGroupId(UUID classGroupId);
    List<ClassSession> findBySessionDate(LocalDate sessionDate);
    List<ClassSession> findByStatus(ClassSessionStatus status);
}
