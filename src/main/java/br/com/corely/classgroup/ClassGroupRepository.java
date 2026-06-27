package br.com.corely.classgroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassGroupRepository extends JpaRepository<ClassGroup, UUID> {
    long countByStudioIdAndActiveTrue(UUID studioId);

    @Query("SELECT COALESCE(SUM(cg.capacity), 0) FROM ClassGroup cg WHERE cg.studio.id = :studioId AND cg.active = true")
    long sumCapacityByStudioIdAndActiveTrue(@Param("studioId") UUID studioId);

    List<ClassGroup> findByInstructorId(UUID instructorId);

    List<ClassGroup> findByInstructorIdAndActiveTrue(UUID instructorId);

    List<ClassGroup> findByActiveTrue();
}
