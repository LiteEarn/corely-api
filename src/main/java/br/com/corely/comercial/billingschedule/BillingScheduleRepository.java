package br.com.corely.comercial.billingschedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingScheduleRepository extends JpaRepository<BillingSchedule, UUID> {

    List<BillingSchedule> findAllByOrderByCreatedAtDesc();

    Optional<BillingSchedule> findByStudentPlanId(UUID studentPlanId);
}
