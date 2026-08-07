package br.com.corely.finance.payment;

import br.com.corely.finance.installment.InstallmentStatus;
import br.com.corely.finance.installment.ReceivableInstallment;
import br.com.corely.finance.installment.ReceivableInstallmentRepository;
import br.com.corely.finance.movement.MovementType;
import br.com.corely.finance.movement.ReceivableMovementService;
import br.com.corely.finance.payment.dto.PaymentMethodDto;
import br.com.corely.finance.payment.dto.PaymentRequest;
import br.com.corely.finance.payment.dto.PaymentResponse;
import br.com.corely.finance.receivable.Receivable;
import br.com.corely.finance.receivable.ReceivableRepository;
import br.com.corely.finance.receivable.ReceivableStatus;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.shared.tenant.TenantContext;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Serviço de pagamentos — baixa manual (EPIC-03-S06).
 *
 * <p>Registra a liquidação de um recebível (ou de uma parcela específica) por
 * baixa manual, atualizando a situação do título para paga. A operação é
 * sempre restrita ao estúdio corrente (multi-tenant via {@link TenantContext})
 * e registra uma movimentação {@code PAYMENT} no histórico do recebível.</p>
 */
@Service("financePaymentService")
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReceivableRepository receivableRepository;
    private final ReceivableInstallmentRepository installmentRepository;
    private final ReceivableMovementService movementService;
    private final StudioRepository studioRepository;
    private final TenantContext tenantContext;

    /**
     * Registra a baixa manual de um recebível (ou parcela).
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>o recebível deve existir no estúdio corrente;</li>
     *   <li>o recebível deve estar {@code OPEN} (não pago nem cancelado);</li>
     *   <li>se uma parcela for informada, ela deve pertencer ao recebível e
     *       estar {@code OPEN};</li>
     *   <li>o valor deve ser igual ao valor do recebível (ou da parcela);</li>
     *   <li>um recebível não pode ser liquidado mais de uma vez.</li>
     * </ul>
     *
     * @param request dados da baixa manual
     * @return pagamento registrado
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public PaymentResponse create(PaymentRequest request) {
        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var receivable = receivableRepository.findById(request.getReceivableId())
                .orElseThrow(() -> new ResourceNotFoundException("Receivable not found"));

        if (receivable.getStatus() != ReceivableStatus.OPEN) {
            throw new BusinessException("Cannot settle a receivable that is not OPEN");
        }
        if (paymentRepository.existsByReceivableId(request.getReceivableId())) {
            throw new BusinessException("Receivable already has a payment");
        }

        ReceivableInstallment installment = null;
        if (request.getInstallmentId() != null) {
            installment = installmentRepository.findById(request.getInstallmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Installment not found"));
            if (!installment.getReceivable().getId().equals(receivable.getId())) {
                throw new BusinessException("Installment does not belong to the receivable");
            }
            if (installment.getStatus() != InstallmentStatus.OPEN) {
                throw new BusinessException("Cannot settle an installment that is not OPEN");
            }
            requireEqualAmount(request.getAmount(), installment.getAmount(), "installment");
        } else {
            requireEqualAmount(request.getAmount(), receivable.getAmount(), "receivable");
        }

        var payment = new Payment();
        payment.setStudio(studio);
        payment.setReceivable(receivable);
        payment.setInstallment(installment);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().name()));
        payment.setExternalReference(request.getExternalReference());
        payment.setNotes(request.getNotes());
        payment = save(payment);

        settle(receivable, installment);

        movementService.record(receivable.getId(), studio.getId(), MovementType.PAYMENT,
                payment.getAmount(), "Pagamento recebido via " + payment.getPaymentMethod());

        return toResponse(payment);
    }

    /**
     * Persiste o pagamento. Proteção contra dupla baixa concorrente: a
     * constraint de unicidade em {@code receivable_id} transforma a violação
     * em {@link BusinessException} (409) em vez de erro interno.
     */
    private Payment save(Payment payment) {
        try {
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Receivable already has a payment");
        }
    }

    /**
     * Atualiza a situação após a baixa: a parcela informada vai para paga e,
     * quando não há mais parcelas em aberto (ou a baixa é do recebível sem
     * parcela), o recebível vai para pago.
     */
    private void settle(Receivable receivable, ReceivableInstallment installment) {
        if (installment != null) {
            installment.setStatus(InstallmentStatus.PAID);
            installmentRepository.save(installment);
            if (installmentRepository
                    .countByReceivableIdAndStatus(receivable.getId(), InstallmentStatus.OPEN) == 0) {
                receivable.setStatus(ReceivableStatus.PAID);
                receivableRepository.save(receivable);
            }
            return;
        }
        receivable.setStatus(ReceivableStatus.PAID);
        receivableRepository.save(receivable);
        installmentRepository.findByReceivableIdAndStatus(receivable.getId(), InstallmentStatus.OPEN)
                .forEach(open -> {
                    open.setStatus(InstallmentStatus.PAID);
                    installmentRepository.save(open);
                });
    }

    private void requireEqualAmount(BigDecimal actual, BigDecimal expected, String target) {
        if (actual.compareTo(expected) != 0) {
            throw new BusinessException("Payment amount must equal the " + target + " amount");
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID id) {
        var payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> findAll(Pageable pageable) {
        UUID studioId = tenantContext.getCurrentStudioId();
        return paymentRepository.findByStudioId(studioId, pageable).map(this::toResponse);
    }

    /**
     * Lista os pagamentos do estúdio corrente filtrados pela forma de
     * pagamento (ex.: {@code CASH}), paginado por data decrescente.
     *
     * <p>Usado pelo fluxo dedicado de pagamento em dinheiro (EPIC-03-S09).</p>
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> findAllByMethod(PaymentMethodDto paymentMethod, Pageable pageable) {
        UUID studioId = tenantContext.getCurrentStudioId();
        return paymentRepository
                .findByStudioIdAndPaymentMethod(studioId, PaymentMethod.valueOf(paymentMethod.name()), pageable)
                .map(this::toResponse);
    }

    private PaymentResponse toResponse(Payment payment) {
        var receivable = payment.getReceivable();
        return new PaymentResponse(
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
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}