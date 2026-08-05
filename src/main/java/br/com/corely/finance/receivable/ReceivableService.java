package br.com.corely.finance.receivable;

import br.com.corely.finance.receivable.dto.ReceivableRequest;
import br.com.corely.finance.receivable.dto.ReceivableResponse;
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
 * Serviço de contas a receber — recebíveis (EPIC-03-S01).
 *
 * <p>Cria e consulta recebíveis (títulos a receber de alunos). A consulta é
 * sempre restrita ao estúdio corrente (multi-tenant via {@link TenantContext}).</p>
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

    @Transactional(readOnly = true)
    public Page<ReceivableResponse> findAll(ReceivableStatus status, UUID studentId,
                                            LocalDate dueDateFrom, LocalDate dueDateTo,
                                            Pageable pageable) {
        if (dueDateFrom != null && dueDateTo != null && dueDateFrom.isAfter(dueDateTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dueDateFrom must not be after dueDateTo");
        }
        UUID studioId = tenantContext.getCurrentStudioId();
        return receivableRepository
                .findByFilters(studioId, status, studentId, dueDateFrom, dueDateTo, pageable)
                .map(this::toResponse);
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
                receivable.getCreatedAt(),
                receivable.getUpdatedAt()
        );
    }
}
