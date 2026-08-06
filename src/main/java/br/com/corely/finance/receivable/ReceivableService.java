package br.com.corely.finance.receivable;

import br.com.corely.finance.receivable.dto.ReceivableRequest;
import br.com.corely.finance.receivable.dto.ReceivableResponse;
import br.com.corely.finance.situation.Situation;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.shared.tenant.TenantContext;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Serviço de contas a receber — recebíveis (EPIC-03-S01/S03).
 *
 * <p>Cria e consulta recebíveis (títulos a receber de alunos). A consulta é
 * sempre restrita ao estúdio corrente (multi-tenant via {@link TenantContext}).
 * A situação financeira (em aberto, paga, vencida, estornada) é derivada do
 * status e do vencimento (EPIC-03-S03).</p>
 */
@Service("receivableService")
@RequiredArgsConstructor
public class ReceivableService {

    private final ReceivableRepository receivableRepository;
    private final StudentRepository studentRepository;
    private final StudioRepository studioRepository;
    private final TenantContext tenantContext;

    @Transactional
    public ReceivableResponse create(ReceivableRequest request) {
        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        var receivable = new Receivable();
        receivable.setStudio(studio);
        receivable.setStudent(student);
        receivable.setDescription(request.getDescription());
        receivable.setAmount(request.getAmount());
        receivable.setDueDate(request.getDueDate());
        receivable.setStatus(ReceivableStatus.OPEN);

        receivable = receivableRepository.save(receivable);
        return toResponse(receivable);
    }

    @Transactional(readOnly = true)
    public ReceivableResponse findById(UUID id) {
        var receivable = receivableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receivable not found"));
        return toResponse(receivable);
    }

    /**
     * Atualiza (reagenda) a data de vencimento de um recebível (EPIC-03-S04).
     *
     * <p>Não permite reagendar um recebível já pago ou estornado.</p>
     *
     * @param id      identificador do recebível
     * @param dueDate nova data de vencimento
     * @return recebível atualizado
     */
    @Transactional
    public ReceivableResponse updateDueDate(UUID id, LocalDate dueDate) {
        var receivable = receivableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receivable not found"));
        if (receivable.getStatus() == ReceivableStatus.PAID) {
            throw new BusinessException("Cannot change due date of a paid receivable");
        }
        if (receivable.getStatus() == ReceivableStatus.CANCELLED) {
            throw new BusinessException("Cannot change due date of a cancelled receivable");
        }
        receivable.setDueDate(dueDate);
        receivable = receivableRepository.save(receivable);
        return toResponse(receivable);
    }

    @Transactional(readOnly = true)
    public Page<ReceivableResponse> findAll(ReceivableStatus status, UUID studentId,
                                            LocalDate dueDateFrom, LocalDate dueDateTo,
                                            Pageable pageable) {
        validateDateRange(dueDateFrom, dueDateTo);
        UUID studioId = tenantContext.getCurrentStudioId();
        return receivableRepository
                .findByFilters(studioId, status, studentId, dueDateFrom, dueDateTo, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReceivableResponse> findBySituation(Situation situation, UUID studentId,
                                                    LocalDate dueDateFrom, LocalDate dueDateTo,
                                                    Pageable pageable) {
        validateDateRange(dueDateFrom, dueDateTo);
        UUID studioId = tenantContext.getCurrentStudioId();
        var status = resolveStatus(situation);
        var overdue = resolveOverdue(situation);
        return receivableRepository
                .findBySituation(studioId, status, overdue, studentId, dueDateFrom, dueDateTo,
                        LocalDate.now(), pageable)
                .map(this::toResponse);
    }

    /**
     * Resolve o status persistido correspondente à situação filtrada.
     */
    private ReceivableStatus resolveStatus(Situation situation) {
        return switch (situation) {
            case PAID -> ReceivableStatus.PAID;
            case REVERSED -> ReceivableStatus.CANCELLED;
            default -> ReceivableStatus.OPEN;
        };
    }

    /**
     * Resolve a flag de vencimento: {@code null} ignora, {@code true} somente
     * vencidos, {@code false} somente não vencidos.
     */
    private Boolean resolveOverdue(Situation situation) {
        return switch (situation) {
            case OVERDUE -> true;
            case OPEN -> false;
            default -> null;
        };
    }

    private void validateDateRange(LocalDate dueDateFrom, LocalDate dueDateTo) {
        if (dueDateFrom != null && dueDateTo != null && dueDateFrom.isAfter(dueDateTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dueDateFrom must not be after dueDateTo");
        }
    }

    private ReceivableResponse toResponse(Receivable receivable) {
        return new ReceivableResponse(
                receivable.getId(),
                receivable.getStudent().getId(),
                receivable.getStudent().getFullName(),
                receivable.getDescription(),
                receivable.getAmount(),
                receivable.getDueDate(),
                receivable.getStatus(),
                Situation.from(receivable.getStatus().name(), receivable.getDueDate()),
                receivable.getCreatedAt(),
                receivable.getUpdatedAt()
        );
    }
}
