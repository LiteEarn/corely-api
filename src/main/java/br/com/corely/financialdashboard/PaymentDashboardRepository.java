package br.com.corely.financialdashboard;

import br.com.corely.comercial.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentDashboardRepository extends JpaRepository<Payment, UUID> {

    @Query("SELECT p.paymentDate, COUNT(p), COALESCE(SUM(p.amount), 0) " +
           "FROM Payment p " +
           "WHERE p.paymentDate >= :dateStart AND p.paymentDate <= :dateEnd " +
           "GROUP BY p.paymentDate ORDER BY p.paymentDate")
    List<Object[]> paymentsPerDay(@Param("dateStart") LocalDate dateStart, @Param("dateEnd") LocalDate dateEnd);

    @Query("SELECT i.referenceMonth, COUNT(p), COALESCE(SUM(p.amount), 0) " +
           "FROM Payment p JOIN p.invoice i " +
           "WHERE i.referenceMonth >= :referenceMonth " +
           "GROUP BY i.referenceMonth ORDER BY i.referenceMonth")
    List<Object[]> paymentsPerMonth(@Param("referenceMonth") String referenceMonth);

    @Query("SELECT COUNT(p), COALESCE(SUM(p.amount), 0) " +
           "FROM Payment p " +
           "WHERE p.paymentDate >= :dateStart AND p.paymentDate <= :dateEnd")
    Object[] countAndSumPayments(@Param("dateStart") LocalDate dateStart, @Param("dateEnd") LocalDate dateEnd);
}
