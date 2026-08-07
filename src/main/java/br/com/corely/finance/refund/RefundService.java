package br.com.corely.finance.refund;

import br.com.corely.finance.installment.InstallmentStatus;
import br.com.corely.finance.installment.ReceivableInstallmentRepository;
import br.com.corely.finance.movement.MovementType;
import br.com.corely.finance.movement.ReceivableMovementService;
import br.com.corely.finance.payment.Payment;
import br.com.corely.finance.payment.PaymentRepository;
import br.com.corely.finance.payment.dto.PaymentMethodDto;
import br.com.corely.finance.receivable.Receivable;
import br.com.corely.finance.receivable.ReceivableRepository;
import br.com.corely.finance.receivable.ReceivableStatus;
import br.com.corely.finance.refund.dto.RefundRequest;
import br.com.corely.finance.refund.dto.RefundResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Serviço de estorno de pagamentos (EPIC-03-S10).
 *
 * <p>Reverte uma baixa manual: o recebível (e a parcela, quando houver) volta à
 * situação {@code OPEN}, o pagamento é marcado como estornado ({@code refundedAt})
 * e uma movimentação {@code REFUND} é registrada no histórico. A operação é
 * sempre restrita ao estúdio corrente (multi-tenant via {@link TenantContext}).</p>
 */
@Service("financeRefundService")
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final ReceivableRepository receivableRepository;
    private final ReceivableInstallmentRepository installmentRepository;
    private final ReceivableMovementService movementService;
    private final TenantContext tenantContext;

    /**
     * Estorna um pagamento registrado na baixa manual.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>o pagamento deve existir no estúdio corrente;</li>
     *   <li>o pagamento não pode estar estornado;</li>
     *   <li>o recebível associado volta para {@code OPEN} (e a parcela, quando
     *       houver).</li>
     * </ul>
     *
     * @param request dados do estorno (pagamento e motivo opcional)
     * @return pagamento estornado
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public RefundResponse create(RefundRequest request) {
        var payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getRefundedAt() != null) {
            throw new BusinessException("Payment already refunded");
        }

        UUID studioId = tenantContext.getCurrentStudioId();
        var receivable = payment.getReceivable();
        revertStatus(receivable, payment);
        movementService.record(receivable.getId(), studioId, MovementType.REFUND,
                payment.getAmount(), request.getReason() != null ? request.getReason() : "Estorno do pagamento");

        payment.setRefundedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        return toResponse(payment, request.getReason());
    }

    /**
     * Reverte a situação após o estorno: o recebível volta para {@code OPEN}.
     * Quando a baixa foi de uma parcela específica, a parcela volta para
     * {@code OPEN}; caso contrário, todas as parcelas pagas do recebível voltam
     * para {@code OPEN}.
     */
    private void revertStatus(Receivable receivable, Payment payment) {
        if (payment.getInstallment() != null) {
            var installment = payment.getInstallment();
            installment.setStatus(InstallmentStatus.OPEN);
            installmentRepository.save(installment);
            if (receivable.getStatus() == ReceivableStatus.PAID) {
                receivable.setStatus(ReceivableStatus.OPEN);
                receivableRepository.save(receivable);
            }
            return;
        }
        if (receivable.getStatus() == ReceivableStatus.PAID) {
            receivable.setStatus(ReceivableStatus.OPEN);
            receivableRepository.save(receivable);
        }
        installmentRepository.findByReceivableIdAndStatus(receivable.getId(), InstallmentStatus.PAID)
                .forEach(paid -> {
                    paid.setStatus(InstallmentStatus.OPEN);
                    installmentRepository.save(paid);
                });
    }

    /**
     * Lista os pagamentos estornados do estúdio corrente, por data de estorno
     * decrescente.
     */
    @Transactional(readOnly = true)
    public Page<RefundResponse> findAll(Pageable pageable) {
        UUID studioId = tenantContext.getCurrentStudioId();
        return paymentRepository.findRefundedByStudioId(studioId, pageable)
                .map(p -> toResponse(p, null));
    }

    private RefundResponse toResponse(Payment payment, String reason) {
        var receivable = payment.getReceivable();
        return new RefundResponse(
                payment.getId(),
                receivable.getId(),
                payment.getInstallment() != null ? payment.getInstallment().getId() : null,
                receivable.getStudent().getId(),
                receivable.getStudent().getFullName(),
                payment.getPaymentDate(),
                payment.getAmount(),
                PaymentMethodDto.valueOf(payment.getPaymentMethod().name()),
                payment.getExternalReference(),
                payment.getNotes(),
                reason,
                payment.getRefundedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
