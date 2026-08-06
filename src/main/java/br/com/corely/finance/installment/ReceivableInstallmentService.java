package br.com.corely.finance.installment;

import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.finance.installment.dto.InstallmentResponse;
import br.com.corely.finance.receivable.Receivable;
import br.com.corely.finance.receivable.ReceivableRepository;
import br.com.corely.finance.situation.Situation;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de parcelas de recebíveis (EPIC-03-S02).
 *
 * <p>Gera as parcelas mensais de um aluno a partir da matrícula/plano
 * ({@link StudentPlan}), desdobrando o valor total em N parcelas com vencimentos
 * mensais, e consulta as parcelas do estúdio corrente (multi-tenant).</p>
 */
@Service("receivableInstallmentService")
@RequiredArgsConstructor
public class ReceivableInstallmentService {

    /**
     * Dias por parcela (mensal).
     */
    private static final int DAYS_PER_INSTALLMENT = 30;

    private final ReceivableInstallmentRepository installmentRepository;
    private final ReceivableRepository receivableRepository;
    private final TenantContext tenantContext;

    /**
     * Gera o recebível mestre e as parcelas mensais a partir da matrícula/plano.
     * O número de parcelas é derivado da duração do plano (em dias) e o valor
     * total é o preço mensal × número de parcelas.
     *
     * @param studentPlan matrícula/plano de origem
     * @return parcelas geradas
     */
    @Transactional
    public List<ReceivableInstallment> generateForStudentPlan(StudentPlan studentPlan) {
        var snapshot = studentPlan.getContractSnapshot();
        int installmentCount = Math.max(1, snapshot.getPlanDuration() / DAYS_PER_INSTALLMENT);
        BigDecimal monthlyPrice = snapshot.getPlanPrice();
        BigDecimal totalAmount = monthlyPrice.multiply(BigDecimal.valueOf(installmentCount));

        var receivable = new Receivable();
        receivable.setStudio(studentPlan.getStudio());
        receivable.setStudent(studentPlan.getStudent());
        receivable.setDescription(snapshot.getPlanName());
        receivable.setAmount(totalAmount);
        receivable.setDueDate(studentPlan.getStartDate());
        receivable.setStatus(br.com.corely.finance.receivable.ReceivableStatus.OPEN);
        receivable = receivableRepository.save(receivable);

        return generate(studentPlan, receivable, installmentCount, studentPlan.getStartDate());
    }

    /**
     * Gera as parcelas mensais de uma matrícula/plano, vinculadas a um
     * recebível mestre. O valor total do recebível é desdobrado em
     * {@code installmentCount} parcelas iguais, com vencimento mensal a partir
     * de {@code startDate}.
     *
     * @param studentPlan       matrícula/plano de origem
     * @param receivable        recebível mestre
     * @param installmentCount  número de parcelas
     * @param startDate         data do primeiro vencimento
     * @return parcelas geradas
     */
    @Transactional
    public List<ReceivableInstallment> generate(StudentPlan studentPlan, Receivable receivable,
                                                int installmentCount, LocalDate startDate) {
        if (installmentCount <= 0) {
            return List.of();
        }
        BigDecimal total = receivable.getAmount();
        BigDecimal baseAmount = total.divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_UP);
        BigDecimal lastAmount = total.subtract(baseAmount.multiply(BigDecimal.valueOf(installmentCount - 1)));

        List<ReceivableInstallment> installments = new ArrayList<>(installmentCount);
        for (int i = 1; i <= installmentCount; i++) {
            var installment = new ReceivableInstallment();
            installment.setStudio(studentPlan.getStudio());
            installment.setReceivable(receivable);
            installment.setStudentPlan(studentPlan);
            installment.setInstallmentNumber(i);
            installment.setAmount(i == installmentCount ? lastAmount : baseAmount);
            installment.setDueDate(startDate.plusMonths(i - 1L));
            installment.setStatus(InstallmentStatus.OPEN);
            installments.add(installmentRepository.save(installment));
        }
        return installments;
    }

    @Transactional(readOnly = true)
    public InstallmentResponse findById(UUID id) {
        var installment = installmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found"));
        return toResponse(installment);
    }

    /**
     * Atualiza (reagenda) a data de vencimento de uma parcela (EPIC-03-S04).
     *
     * <p>Não permite reagendar uma parcela já paga ou estornada.</p>
     *
     * @param id      identificador da parcela
     * @param dueDate nova data de vencimento
     * @return parcela atualizada
     */
    @Transactional
    public InstallmentResponse updateDueDate(UUID id, LocalDate dueDate) {
        var installment = installmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found"));
        if (installment.getStatus() == InstallmentStatus.PAID) {
            throw new BusinessException("Cannot change due date of a paid installment");
        }
        if (installment.getStatus() == InstallmentStatus.CANCELLED) {
            throw new BusinessException("Cannot change due date of a cancelled installment");
        }
        installment.setDueDate(dueDate);
        installment = installmentRepository.save(installment);
        return toResponse(installment);
    }

    @Transactional(readOnly = true)
    public Page<InstallmentResponse> findAll(InstallmentStatus status, UUID studentPlanId,
                                             LocalDate dueDateFrom, LocalDate dueDateTo,
                                             Pageable pageable) {
        validateDateRange(dueDateFrom, dueDateTo);
        UUID studioId = tenantContext.getCurrentStudioId();
        return installmentRepository
                .findByFilters(studioId, status, studentPlanId, dueDateFrom, dueDateTo, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<InstallmentResponse> findBySituation(Situation situation, UUID studentPlanId,
                                                     LocalDate dueDateFrom, LocalDate dueDateTo,
                                                     Pageable pageable) {
        validateDateRange(dueDateFrom, dueDateTo);
        UUID studioId = tenantContext.getCurrentStudioId();
        var status = resolveStatus(situation);
        var overdue = resolveOverdue(situation);
        return installmentRepository
                .findBySituation(studioId, status, overdue, studentPlanId, dueDateFrom, dueDateTo,
                        LocalDate.now(), pageable)
                .map(this::toResponse);
    }

    private InstallmentStatus resolveStatus(Situation situation) {
        return switch (situation) {
            case PAID -> InstallmentStatus.PAID;
            case REVERSED -> InstallmentStatus.CANCELLED;
            default -> InstallmentStatus.OPEN;
        };
    }

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

    private InstallmentResponse toResponse(ReceivableInstallment installment) {
        var studentPlan = installment.getStudentPlan();
        var student = studentPlan.getStudent();
        return new InstallmentResponse(
                installment.getId(),
                installment.getReceivable().getId(),
                studentPlan.getId(),
                student.getId(),
                student.getFullName(),
                installment.getInstallmentNumber(),
                installment.getAmount(),
                installment.getDueDate(),
                installment.getStatus(),
                Situation.from(installment.getStatus().name(), installment.getDueDate()),
                installment.getCreatedAt(),
                installment.getUpdatedAt()
        );
    }
}
