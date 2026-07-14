package br.com.corely.comercial.schedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    @Query("SELECT COUNT(s) > 0 FROM Schedule s WHERE s.studio.id = :studioId AND s.name = :name")
    boolean existsByStudioIdAndName(@Param("studioId") UUID studioId, @Param("name") String name);

    @Query("SELECT COUNT(s) > 0 FROM Schedule s WHERE s.studio.id = :studioId AND s.name = :name AND s.id <> :id")
    boolean existsByStudioIdAndNameAndIdNot(@Param("studioId") UUID studioId, @Param("name") String name, @Param("id") UUID id);

    Page<Schedule> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Schedule> findByActive(Boolean active, Pageable pageable);

    Page<Schedule> findByNameContainingIgnoreCaseAndActive(String name, Boolean active, Pageable pageable);

    @Override
    @Query("SELECT s FROM Schedule s WHERE s.id = :id")
    Optional<Schedule> findById(@Param("id") UUID id);
}
