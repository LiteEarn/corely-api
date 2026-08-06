package br.com.corely.finance.movement;

import br.com.corely.finance.movement.dto.MovementResponse;
import br.com.corely.finance.receivable.Receivable;
import br.com.corely.finance.receivable.ReceivableRepository;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.shared.tenant.TenantContext;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Serviço do histórico de movimentações de recebíveis (EPIC-03-S05).
 *
 * <p>Registra eventos do ciclo de vida do recebível (criação, pagamento, ajuste,
 * cancelamento, mudança de vencimento) e consulta o histórico restrito ao
 * estúdio corrente (multi-tenant).</p>
 */
@Service("receivableMovementService")
@RequiredArgsConstructor
public class ReceivableMovementService {

    private final ReceivableMovementRepository movementRepository;
    private final ReceivableRepository receivableRepository;
    private final StudioRepository studioRepository;
    private final TenantContext tenantContext;

    /**
     * Registra uma movimentação no histórico de um recebível.
     *
     * @param receivableId  identificador do recebível
     * @param studioId      estúdio ao qual o recebível pertence
     * @param movementType  tipo de movimentação
     * @param amount        valor da movimentação (pode ser nulo)
     * @param description   descrição (pode ser nula)
     */
    @Transactional
    public void record(UUID receivableId, UUID studioId, MovementType movementType,
                       BigDecimal amount, String description) {
        Receivable receivableRef = new Receivable();
        receivableRef.setId(receivableId);

        var movement = new ReceivableMovement();
        movement.setStudio(studioRepository.getReferenceById(studioId));
        movement.setReceivable(receivableRef);
        movement.setMovementType(movementType);
        movement.setAmount(amount);
        movement.setDescription(description);
        movement.setOccurredAt(LocalDateTime.now());
        movementRepository.save(movement);
    }

    /**
     * Consulta o histórico de movimentações de um recebível do estúdio corrente.
     *
     * @param receivableId identificador do recebível
     * @param pageable     paginação
     * @return página de movimentações, ordenada por data decrescente
     * @throws ResourceNotFoundException se o recebível não existir no estúdio corrente
     */
    @Transactional(readOnly = true)
    public Page<MovementResponse> findByReceivableId(UUID receivableId, Pageable pageable) {
        UUID studioId = tenantContext.getCurrentStudioId();
        receivableRepository.findById(receivableId)
                .orElseThrow(() -> new ResourceNotFoundException("Receivable not found"));
        return movementRepository.findByReceivableId(studioId, receivableId, pageable)
                .map(this::toResponse);
    }

    private MovementResponse toResponse(ReceivableMovement movement) {
        return new MovementResponse(
                movement.getId(),
                movement.getReceivable().getId(),
                movement.getMovementType(),
                movement.getAmount(),
                movement.getDescription(),
                movement.getOccurredAt()
        );
    }
}
