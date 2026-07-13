package br.com.corely.comercial.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findAllByOrderByCreatedAtDesc();

    Optional<Payment> findByInvoiceId(UUID invoiceId);
}
