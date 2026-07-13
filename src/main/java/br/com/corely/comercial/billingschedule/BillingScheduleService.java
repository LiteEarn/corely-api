package br.com.corely.comercial.billingschedule;

import br.com.corely.comercial.billingschedule.dto.BillingFrequencyDto;
import br.com.corely.comercial.billingschedule.dto.BillingScheduleRequest;
import br.com.corely.comercial.billingschedule.dto.BillingScheduleResponse;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingScheduleService {

    private final BillingScheduleRepository billingScheduleRepository;
    private final StudentPlanRepository studentPlanRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public BillingScheduleResponse create(StudentPlan studentPlan, Integer billingDay) {
        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var schedule = new BillingSchedule();
        schedule.setStudio(studio);
        schedule.setStudentPlan(studentPlan);
        schedule.setFrequency(BillingFrequency.MONTHLY);
        schedule.setBillingDay(billingDay);
        schedule.setNextBillingDate(calculateNextBillingDate(studentPlan.getStartDate(), BillingFrequency.MONTHLY, billingDay));
        schedule.setActive(true);

        schedule = billingScheduleRepository.save(schedule);
        return toResponse(schedule);
    }

    @Transactional
    public BillingScheduleResponse update(UUID id, BillingScheduleRequest request) {
        var schedule = billingScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BillingSchedule not found"));

        var frequency = BillingFrequency.valueOf(request.getFrequency().name());
        schedule.setFrequency(frequency);
        schedule.setBillingDay(request.getBillingDay());
        schedule.setNextBillingDate(calculateNextBillingDate(
                schedule.getStudentPlan().getStartDate(), frequency, request.getBillingDay()));

        if (request.getActive() != null) {
            validateActivation(schedule.getStudentPlan(), request.getActive());
            schedule.setActive(request.getActive());
        }

        schedule = billingScheduleRepository.save(schedule);
        return toResponse(schedule);
    }

    @Transactional(readOnly = true)
    public BillingScheduleResponse findById(UUID id) {
        var schedule = billingScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BillingSchedule not found"));
        return toResponse(schedule);
    }

    @Transactional(readOnly = true)
    public List<BillingScheduleResponse> findAll() {
        return billingScheduleRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateActivation(StudentPlan studentPlan, Boolean active) {
        if (active && studentPlan.getStatus() != StudentPlanStatus.ACTIVE) {
            throw new BusinessException("Cannot activate billing schedule for a non-active student plan.");
        }
    }

    static LocalDate calculateNextBillingDate(LocalDate startDate, BillingFrequency frequency, Integer billingDay) {
        return switch (frequency) {
            case WEEKLY -> startDate.withDayOfMonth(Math.min(billingDay, startDate.lengthOfMonth()));
            case BIWEEKLY -> startDate.plusWeeks(2);
            case MONTHLY -> startDate.withDayOfMonth(Math.min(billingDay, startDate.lengthOfMonth()));
            case QUARTERLY -> startDate.plusMonths(3);
            case SEMIANNUAL -> startDate.plusMonths(6);
            case ANNUAL -> startDate.plusYears(1);
        };
    }

    private BillingScheduleResponse toResponse(BillingSchedule schedule) {
        var sp = schedule.getStudentPlan();
        var student = sp.getStudent();
        var snapshot = sp.getContractSnapshot();
        return new BillingScheduleResponse(
                schedule.getId(),
                sp.getId(),
                student.getFullName(),
                snapshot.getPlanName(),
                BillingFrequencyDto.valueOf(schedule.getFrequency().name()),
                schedule.getBillingDay(),
                schedule.getNextBillingDate(),
                schedule.getActive(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
