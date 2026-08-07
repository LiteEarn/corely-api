package br.com.corely.finance.pix;

import br.com.corely.finance.payment.PaymentService;
import br.com.corely.finance.payment.PaymentRepository;
import br.com.corely.finance.payment.dto.PaymentMethodDto;
import br.com.corely.finance.payment.dto.PaymentRequest;
import br.com.corely.finance.pix.dto.PixPaymentRequest;
import br.com.corely.finance.pix.dto.PixPaymentResponse;
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
 * Serviço de cobranças Pix (EPIC-03-S07).
 *
 * <p>Gera uma cobrança Pix para um recebível em aberto do estúdio corrente e
 * permite a conciliação (confirmação) do pagamento. A confirmação reutiliza o
 * {@link PaymentService} para registrar a baixa (método {@code PIX}), liquidar
 * o recebível e registrar a movimentação no histórico.</p>
 */
@Service("financePixPaymentService")
@RequiredArgsConstructor
public class PixPaymentService {

    private static final int DEFAULT_EXPIRATION_HOURS = 24;

    private final PixPaymentRepository pixPaymentRepository;
    private final ReceivableRepository receivableRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final StudioRepository studioRepository;
    private final TenantContext tenantContext;

    /**
     * Gera uma cobrança Pix para um recebível em aberto.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>o recebível deve existir no estúdio corrente;</li>
     *   <li>o recebível deve estar {@code OPEN};</li>
     *   <li>o recebível não pode ter pagamento prévio;</li>
     *   <li>não pode existir cobrança Pix pendente para o mesmo recebível.</li>
     * </ul>
     *
     * @param request dados da cobrança (recebível e validade opcional)
     * @return cobrança Pix gerada
     */
    @Transactional
    public PixPaymentResponse create(PixPaymentRequest request) {
        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var receivable = receivableRepository.findById(request.getReceivableId())
                .orElseThrow(() -> new ResourceNotFoundException("Receivable not found"));

        if (receivable.getStatus() != ReceivableStatus.OPEN) {
            throw new BusinessException("Cannot create a Pix charge for a receivable that is not OPEN");
        }
        if (paymentRepository.existsByReceivableId(receivable.getId())) {
            throw new BusinessException("Receivable already has a payment");
        }
        if (pixPaymentRepository.existsByReceivableId(receivable.getId())) {
            throw new BusinessException("Receivable already has a Pix charge");
        }

        String txid = generateTxid();
        var pix = new PixPayment();
        pix.setStudio(studio);
        pix.setReceivable(receivable);
        pix.setTxid(txid);
        pix.setCopyPaste(generateCopyPaste(txid));
        pix.setAmount(receivable.getAmount());
        pix.setStatus(PixPaymentStatus.PENDING);
        pix.setExpiresAt(request.getExpiresAt() != null
                ? request.getExpiresAt()
                : LocalDateTime.now().plusHours(DEFAULT_EXPIRATION_HOURS));
        pix = save(pix);

        return toResponse(pix);
    }

    /**
     * Persiste a cobrança Pix. Proteção contra geração concorrente para o mesmo
     * recebível: a constraint de unicidade em {@code receivable_id} transforma a
     * violação em {@link BusinessException} (409) em vez de erro interno.
     */
    private PixPayment save(PixPayment pix) {
        try {
            return pixPaymentRepository.saveAndFlush(pix);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Receivable already has a Pix charge");
        }
    }

    /**
     * Concilia uma cobrança Pix pelo {@code txid}: confirma o pagamento,
     * registra a baixa (método {@code PIX}) e liquida o recebível.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>a cobrança deve existir e estar {@link PixPaymentStatus#PENDING};</li>
     *   <li>a cobrança não pode estar expirada;</li>
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
     * @param txid identificador da transação Pix
     * @return cobrança conciliada
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public PixPaymentResponse confirm(String txid) {
        var pix = pixPaymentRepository.findByTxid(txid)
                .orElseThrow(() -> new ResourceNotFoundException("Pix charge not found"));

        if (pix.getStatus() != PixPaymentStatus.PENDING) {
            throw new BusinessException("Pix charge is not pending");
        }
        if (pix.getExpiresAt() != null && pix.getExpiresAt().isBefore(LocalDateTime.now())) {
            pix.setStatus(PixPaymentStatus.EXPIRED);
            pixPaymentRepository.save(pix);
            throw new BusinessException("Pix charge has expired");
        }

        var paymentRequest = new PaymentRequest();
        paymentRequest.setReceivableId(pix.getReceivable().getId());
        paymentRequest.setPaymentDate(LocalDate.now());
        paymentRequest.setAmount(pix.getAmount());
        paymentRequest.setPaymentMethod(PaymentMethodDto.PIX);
        paymentRequest.setExternalReference(pix.getTxid());
        try {
            paymentService.create(paymentRequest);
        } catch (BusinessException ex) {
            pix.setStatus(PixPaymentStatus.CANCELLED);
            pixPaymentRepository.save(pix);
            throw ex;
        }

        pix.setStatus(PixPaymentStatus.PAID);
        pix.setPaidAt(LocalDateTime.now());
        pix = pixPaymentRepository.save(pix);

        return toResponse(pix);
    }

    @Transactional(readOnly = true)
    public PixPaymentResponse findById(UUID id) {
        var pix = pixPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pix charge not found"));
        return toResponse(pix);
    }

    @Transactional(readOnly = true)
    public Page<PixPaymentResponse> findAll(Pageable pageable) {
        UUID studioId = tenantContext.getCurrentStudioId();
        return pixPaymentRepository.findByStudioId(studioId, pageable).map(this::toResponse);
    }

    private PixPaymentResponse toResponse(PixPayment pix) {
        var receivable = pix.getReceivable();
        return new PixPaymentResponse(
                pix.getId(),
                receivable.getId(),
                receivable.getStudent().getId(),
                receivable.getStudent().getFullName(),
                pix.getTxid(),
                pix.getCopyPaste(),
                pix.getAmount(),
                pix.getStatus(),
                pix.getExpiresAt(),
                pix.getPaidAt(),
                pix.getCreatedAt()
        );
    }

    /**
     * Gera o {@code txid} da transação Pix.
     *
     * <p>Identificador único (UUID sem hífens, 32 caracteres) dentro dos 35
     * caracteres permitidos pelo padrão Pix.</p>
     */
    private String generateTxid() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    /**
     * Gera um código copia-e-cola determinístico a partir do {@code txid}.
     *
     * <p>Como a integração com o provedor de pagamento (PSP) está fora do
     * escopo desta story, o código é um placeholder determinístico que carrega
     * o {@code txid} para permitir a conciliação. A substituição por um EMV
     * BR Code real ocorrerá na integração com o PSP.</p>
     */
    private String generateCopyPaste(String txid) {
        return "00020126580014BR.GOV.BCB.PIX0136" + txid + "5204000053039865802BR5909CORELY6009CORELY62070503***6304";
    }
}
