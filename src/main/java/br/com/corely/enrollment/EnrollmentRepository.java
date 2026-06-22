package br.com.corely.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    Optional<Enrollment> findByStudentIdAndClassGroupId(UUID studentId, UUID classGroupId);

    long countByStudioIdAndActiveTrue(UUID studioId);

    long countByStudioIdAndClassGroup_IdAndActiveTrue(UUID studioId, UUID classGroupId);

    List<Enrollment> findByClassGroupIdAndActiveTrueAndStudentActiveTrueAndClassGroupActiveTrue(UUID classGroupId);

    List<Enrollment> findByStudentIdAndActiveTrue(UUID studentId);
}
