package br.com.corely.instructor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, UUID> {
    long countByStudioIdAndActiveTrue(UUID studioId);
    List<Instructor> findByStudioIdAndActiveTrue(UUID studioId);
}
