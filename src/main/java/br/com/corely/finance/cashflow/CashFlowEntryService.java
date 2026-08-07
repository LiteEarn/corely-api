package br.com.corely.finance.cashflow;

import br.com.corely.finance.cashflow.dto.CashFlowEntryRequest;
import br.com.corely.finance.cashflow.dto.CashFlowEntryResponse;
import br.com.corely.finance.cashflow.dto.CashFlowEntrySourceDto;
import br.com.corely.finance.cashflow.dto.CashFlowEntryTypeDto;
import br.com.corely.finance.payment.PaymentRepository;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.shared.tenant.TenantContext;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Serviço de fluxo de caixa — entradas e saídas (EPIC-03-S11/S12).
 *
 * <p>Registra movimentos de caixa (entradas e, no modelo, saídas) do estúdio
 * corrente e permite a consulta filtrada por tipo e período. Entradas originadas
 * de pagamentos referenciam o {@code payment} correspondente (validação de
 * existência no tenant). A operação é sempre restrita ao estúdio corrente
 * (multi-tenant via {@link TenantContext}).</p>
 */
@Service("financeCashFlowEntryService")
@RequiredArgsConstructor
public class CashFlowEntryService {

    private final CashFlowEntryRepository cashFlowEntryRepository;
    private final PaymentRepository paymentRepository;
    private final StudioRepository studioRepository;
    private final TenantContext tenantContext;

    /**
     * Registra um movimento de caixa.
     *
     * <p>Regras:</p>
     * <ul>
     *   <li>o estúdio corrente é sempre o dono do lançamento;</li>
     *   <li>quando a origem é {@code PAYMENT}, o {@code paymentId} é obrigatório
     *       e o pagamento deve existir no estúdio corrente;</li>
     *   <li>quando a origem é {@code MANUAL}, não há pagamento associado;</li>
     *   <li>saídas ({@code OUTFLOW}) nunca são originadas de pagamento — pagamentos
     *       geram entradas; uma saída deve ser lançada manualmente.</li>
     * </ul>
     *
     * @param request dados do movimento de caixa
     * @return movimento registrado
     */
    @Transactional
    public CashFlowEntryResponse create(CashFlowEntryRequest request) {
        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        if (request.getEntryType() == CashFlowEntryTypeDto.OUTFLOW
                && request.getSource() == CashFlowEntrySourceDto.PAYMENT) {
            throw new BusinessException("OUTFLOW entries cannot be sourced from a payment");
        }

        var entry = new CashFlowEntry();
        entry.setStudio(studio);
        entry.setEntryType(CashFlowEntryType.valueOf(request.getEntryType().name()));
        entry.setEntryDate(request.getEntryDate());
        entry.setAmount(request.getAmount());
        entry.setDescription(request.getDescription().trim());
        entry.setSource(CashFlowEntrySource.valueOf(request.getSource().name()));
        entry.setCategory(request.getCategory());
        if (request.getSource() == CashFlowEntrySourceDto.PAYMENT) {
            if (request.getPaymentId() == null) {
                throw new BusinessException("paymentId is required when source is PAYMENT");
            }
            var payment = paymentRepository.findById(request.getPaymentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
            entry.setPayment(payment);
        }
        entry = cashFlowEntryRepository.save(entry);

        return toResponse(entry);
    }

    /**
     * Consulta os movimentos de caixa do estúdio corrente, filtrando por tipo e
     * período (opcionais), ordenado por data decrescente.
     */
    @Transactional(readOnly = true)
    public Page<CashFlowEntryResponse> findAll(CashFlowEntryTypeDto entryType,
                                               LocalDate dateFrom, LocalDate dateTo,
                                               Pageable pageable) {
        UUID studioId = tenantContext.getCurrentStudioId();
        CashFlowEntryType type = entryType != null
                ? CashFlowEntryType.valueOf(entryType.name())
                : null;
        return cashFlowEntryRepository.findByFilters(studioId, type, dateFrom, dateTo, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CashFlowEntryResponse findById(UUID id) {
        var entry = cashFlowEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cash flow entry not found"));
        return toResponse(entry);
    }

    private CashFlowEntryResponse toResponse(CashFlowEntry entry) {
        return new CashFlowEntryResponse(
                entry.getId(),
                CashFlowEntryTypeDto.valueOf(entry.getEntryType().name()),
                entry.getEntryDate(),
                entry.getAmount(),
                entry.getDescription(),
                CashFlowEntrySourceDto.valueOf(entry.getSource().name()),
                entry.getPayment() != null ? entry.getPayment().getId() : null,
                entry.getCategory(),
                entry.getCreatedAt()
        );
    }
}
