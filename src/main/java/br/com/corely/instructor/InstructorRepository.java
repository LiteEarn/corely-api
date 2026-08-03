package br.com.corely.instructor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, UUID> {
    long countByStudioIdAndActiveTrue(UUID studioId);
    List<Instructor> findByStudioIdAndActiveTrue(UUID studioId);

    @Override
    @Query("SELECT i FROM Instructor i WHERE i.id = :id")
    Optional<Instructor> findById(@Param("id") UUID id);
}
