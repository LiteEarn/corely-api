package br.com.corely.finance.movement;

import br.com.corely.finance.movement.dto.MovementResponse;
import br.com.corely.finance.receivable.Receivable;
import br.com.corely.finance.receivable.ReceivableRepository;
import br.com.corely.shared.tenant.TenantContext;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do serviço de histórico de movimentações (EPIC-03-S05).
 */
@ExtendWith(MockitoExtension.class)
class ReceivableMovementServiceTest {

    @Mock
    private ReceivableMovementRepository movementRepository;

    @Mock
    private ReceivableRepository receivableRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private TenantContext tenantContext;

    private ReceivableMovementService service;

    private UUID studioId;
    private Studio studio;

    @BeforeEach
    void setUp() {
        service = new ReceivableMovementService(movementRepository, receivableRepository,
                studioRepository, tenantContext);

        studioId = UUID.randomUUID();
        studio = new Studio();
        studio.setId(studioId);
    }

    @Test
    void record_shouldPersistMovement() {
        UUID receivableId = UUID.randomUUID();
        when(studioRepository.getReferenceById(studioId)).thenReturn(studio);
        when(movementRepository.save(any(ReceivableMovement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.record(receivableId, studioId, MovementType.CREATED, BigDecimal.valueOf(100), "Criado");

        verify(movementRepository).save(any(ReceivableMovement.class));
    }

    @Test
    void findByReceivableId_shouldReturnMovementsOfStudio() {
        UUID receivableId = UUID.randomUUID();
        var movement = new ReceivableMovement();
        movement.setId(UUID.randomUUID());
        Receivable receivableRef = new Receivable();
        receivableRef.setId(receivableId);
        movement.setReceivable(receivableRef);
        movement.setMovementType(MovementType.DUE_DATE_CHANGED);
        movement.setAmount(BigDecimal.valueOf(100));
        movement.setDescription("Vencimento alterado");
        movement.setOccurredAt(LocalDateTime.now());

        var page = new PageImpl<>(List.of(movement), PageRequest.of(0, 10), 1);
        Receivable receivable = new Receivable();
        receivable.setId(receivableId);
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivableId)).thenReturn(Optional.of(receivable));
        when(movementRepository.findByReceivableId(eq(studioId), eq(receivableId), any(PageRequest.class)))
                .thenReturn(page);

        Page<MovementResponse> result = service.findByReceivableId(receivableId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReceivableId()).isEqualTo(receivableId);
        assertThat(result.getContent().get(0).getMovementType()).isEqualTo(MovementType.DUE_DATE_CHANGED);
    }

    @Test
    void findByReceivableId_shouldThrowWhenReceivableNotFound() {
        UUID receivableId = UUID.randomUUID();
        when(tenantContext.getCurrentStudioId()).thenReturn(studioId);
        when(receivableRepository.findById(receivableId)).thenReturn(Optional.empty());

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> service.findByReceivableId(receivableId, PageRequest.of(0, 10))))
                .isInstanceOf(br.com.corely.shared.exception.ResourceNotFoundException.class);
    }
}
