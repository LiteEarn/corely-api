package br.com.corely.financialdashboard;

import br.com.corely.comercial.studentplan.StudentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FinancialDashboardStudentPlanRepository extends JpaRepository<StudentPlan, UUID> {

    @Query("SELECT COUNT(sp) FROM StudentPlan sp WHERE sp.status = 'ACTIVE'")
    long countActive();

    @Query("SELECT COUNT(sp) FROM StudentPlan sp WHERE sp.createdAt >= :monthStart AND sp.createdAt < :monthEnd")
    long countCreatedInMonth(@Param("monthStart") LocalDateTime monthStart, @Param("monthEnd") LocalDateTime monthEnd);

    @Query("SELECT COUNT(sp) FROM StudentPlan sp WHERE sp.cancellationDate >= :dateStart AND sp.cancellationDate < :dateEnd AND sp.status = 'CANCELLED'")
    long countCancelledInMonth(@Param("dateStart") LocalDate dateStart, @Param("dateEnd") LocalDate dateEnd);

    @Query("SELECT cs.planName, cs.planPrice, COUNT(sp) " +
           "FROM StudentPlan sp JOIN sp.contractSnapshot cs " +
           "WHERE sp.status = 'ACTIVE' " +
           "GROUP BY cs.planName, cs.planPrice")
    List<Object[]> countActivePerPlan();
}
