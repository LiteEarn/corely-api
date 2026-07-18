package br.com.corely.finance.membershipplan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, UUID> {

    @Query("SELECT p FROM MembershipPlan p WHERE p.active = true")
    List<MembershipPlan> findAllActive();

    @Query("SELECT p FROM MembershipPlan p WHERE p.studio.id = :studioId AND p.name = :name")
    Optional<MembershipPlan> findByName(@Param("studioId") UUID studioId, @Param("name") String name);

    @Query("SELECT COUNT(p) > 0 FROM MembershipPlan p WHERE p.studio.id = :studioId AND p.name = :name AND p.active = true")
    boolean existsByName(@Param("studioId") UUID studioId, @Param("name") String name);

    @Query("SELECT COUNT(p) > 0 FROM MembershipPlan p WHERE p.studio.id = :studioId AND p.name = :name AND p.active = true AND p.id <> :id")
    boolean existsByNameAndIdNot(@Param("studioId") UUID studioId, @Param("name") String name, @Param("id") UUID id);

    @Override
    @Query("SELECT p FROM MembershipPlan p WHERE p.id = :id")
    Optional<MembershipPlan> findById(@Param("id") UUID id);

    @Query("SELECT p FROM MembershipPlan p WHERE p.studio.id = :studioId ORDER BY p.createdAt ASC")
    MembershipPlan findFirstByStudioId(@Param("studioId") UUID studioId);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.membershipPlan.id = :planId")
    long countStudentsByPlanId(@Param("planId") UUID planId);

    @Query("SELECT p.id, p.name, COUNT(s) FROM MembershipPlan p LEFT JOIN Student s ON s.membershipPlan.id = p.id WHERE p.studio.id = :studioId AND p.active = true GROUP BY p.id, p.name")
    List<Object[]> countStudentsPerPlanByStudioId(@Param("studioId") UUID studioId);

    @Query("SELECT p.id, p.name, p.monthlyPrice, COUNT(s) FROM MembershipPlan p LEFT JOIN Student s ON s.membershipPlan.id = p.id WHERE p.studio.id = :studioId AND p.active = true GROUP BY p.id, p.name, p.monthlyPrice")
    List<Object[]> revenuePerPlanByStudioId(@Param("studioId") UUID studioId);
}
