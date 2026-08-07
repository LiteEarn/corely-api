package br.com.corely.finance.card;

import br.com.corely.finance.payment.PaymentService;
import br.com.corely.finance.payment.PaymentRepository;
import br.com.corely.finance.payment.dto.PaymentMethodDto;
import br.com.corely.finance.payment.dto.PaymentRequest;
import br.com.corely.finance.card.dto.CardPaymentRequest;
import br.com.corely.finance.card.dto.CardPaymentResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Serviço de transações de cartão (EPIC-03-S08).
 *
 * <p>Gera uma transação de cartão para um recebível em aberto do estúdio
 * corrente e permite a conciliação (confirmação) do pagamento. A confirmação
 * reutiliza o {@link PaymentService} para registrar a baixa (método
 * {@code CREDIT_CARD}), liquidar o recebível e registrar a movimentação no
 * histórico.</p>
 */
@Service("financeCardPaymentService")
@RequiredArgsConstructor
public class CardPaymentService {

    private static final int DEFAULT_EXPIRATION_HOURS = 24;

    private final CardPaymentRepository cardPaymentRepository;
    private final ReceivableRepository receivableRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final StudioRepository studioRepository;
    private final TenantContext tenantContext;

    /**
     * Gera uma transação de cartão para um recebível em aberto.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>o recebível deve existir no estúdio corrente;</li>
     *   <li>o recebível deve estar {@code OPEN};</li>
     *   <li>o recebível não pode ter pagamento prévio;</li>
     *   <li>não pode existir transação de cartão pendente para o mesmo recebível.</li>
     * </ul>
     *
     * @param request dados da transação (recebível, bandeira, últimos 4 dígitos,
     *                parcelas e validade opcional)
     * @return transação de cartão gerada
     */
    @Transactional
    public CardPaymentResponse create(CardPaymentRequest request) {
        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var receivable = receivableRepository.findById(request.getReceivableId())
                .orElseThrow(() -> new ResourceNotFoundException("Receivable not found"));

        if (receivable.getStatus() != ReceivableStatus.OPEN) {
            throw new BusinessException("Cannot create a card transaction for a receivable that is not OPEN");
        }
        if (paymentRepository.existsByReceivableId(receivable.getId())) {
            throw new BusinessException("Receivable already has a payment");
        }
        if (cardPaymentRepository.existsByReceivableId(receivable.getId())) {
            throw new BusinessException("Receivable already has a card transaction");
        }

        String transactionId = generateTransactionId();
        var card = new CardPayment();
        card.setStudio(studio);
        card.setReceivable(receivable);
        card.setTransactionId(transactionId);
        card.setCardBrand(request.getCardBrand().trim().toUpperCase());
        card.setLastFourDigits(request.getLastFourDigits());
        card.setInstallments(request.getInstallments() != null ? request.getInstallments() : 1);
        card.setAmount(receivable.getAmount());
        card.setStatus(CardPaymentStatus.PENDING);
        card.setExpiresAt(request.getExpiresAt() != null
                ? request.getExpiresAt()
                : LocalDateTime.now().plusHours(DEFAULT_EXPIRATION_HOURS));
        card = save(card);

        return toResponse(card);
    }

    /**
     * Persiste a transação de cartão. Proteção contra geração concorrente para o
     * mesmo recebível: a constraint de unicidade em {@code receivable_id}
     * transforma a violação em {@link BusinessException} (409) em vez de erro
     * interno.
     */
    private CardPayment save(CardPayment card) {
        try {
            return cardPaymentRepository.saveAndFlush(card);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Receivable already has a card transaction");
        }
    }

    /**
     * Concilia uma transação de cartão pelo {@code transactionId}: confirma o
     * pagamento, registra a baixa (método {@code CREDIT_CARD}) e liquida o
     * recebível.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>a transação deve existir e estar {@link CardPaymentStatus#PENDING};</li>
     *   <li>a transação não pode estar expirada;</li>
     *   <li>o recebível deve estar em aberto e sem pagamento prévio (validado
     *       pelo {@link PaymentService}).</li>
     * </ul>
     *
     * <p>Usa {@code noRollbackFor} para que o estado terminal
     * ({@code CANCELLED}/{@code EXPIRED}) seja persistido mesmo quando a baixa
     * falha: o {@link PaymentService#create} valida todas as regras antes de
     * persistir, portanto lança {@link BusinessException} sem deixar mudanças
     * parciais.</p>
     *
     * @param transactionId identificador da transação de cartão
     * @return transação conciliada
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public CardPaymentResponse confirm(String transactionId) {
        var card = cardPaymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Card transaction not found"));

        if (card.getStatus() != CardPaymentStatus.PENDING) {
            throw new BusinessException("Card transaction is not pending");
        }
        if (card.getExpiresAt() != null && card.getExpiresAt().isBefore(LocalDateTime.now())) {
            card.setStatus(CardPaymentStatus.EXPIRED);
            cardPaymentRepository.save(card);
            throw new BusinessException("Card transaction has expired");
        }

        var paymentRequest = new PaymentRequest();
        paymentRequest.setReceivableId(card.getReceivable().getId());
        paymentRequest.setPaymentDate(LocalDate.now());
        paymentRequest.setAmount(card.getAmount());
        paymentRequest.setPaymentMethod(PaymentMethodDto.CREDIT_CARD);
        paymentRequest.setExternalReference(card.getTransactionId());
        try {
            paymentService.create(paymentRequest);
        } catch (BusinessException ex) {
            card.setStatus(CardPaymentStatus.CANCELLED);
            cardPaymentRepository.save(card);
            throw ex;
        }

        card.setStatus(CardPaymentStatus.PAID);
        card.setPaidAt(LocalDateTime.now());
        card = cardPaymentRepository.save(card);

        return toResponse(card);
    }

    @Transactional(readOnly = true)
    public CardPaymentResponse findById(UUID id) {
        var card = cardPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card transaction not found"));
        return toResponse(card);
    }

    @Transactional(readOnly = true)
    public Page<CardPaymentResponse> findAll(Pageable pageable) {
        UUID studioId = tenantContext.getCurrentStudioId();
        return cardPaymentRepository.findByStudioId(studioId, pageable).map(this::toResponse);
    }

    private CardPaymentResponse toResponse(CardPayment card) {
        var receivable = card.getReceivable();
        return new CardPaymentResponse(
                card.getId(),
                receivable.getId(),
                receivable.getStudent().getId(),
                receivable.getStudent().getFullName(),
                card.getTransactionId(),
                card.getCardBrand(),
                card.getLastFourDigits(),
                card.getInstallments(),
                card.getAmount(),
                card.getStatus(),
                card.getExpiresAt(),
                card.getPaidAt(),
                card.getCreatedAt()
        );
    }

    /**
     * Gera o identificador da transação de cartão.
     *
     * <p>Identificador único (UUID sem hífens, 32 caracteres) usado como
     * referência externa da transação.</p>
     */
    private String generateTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}