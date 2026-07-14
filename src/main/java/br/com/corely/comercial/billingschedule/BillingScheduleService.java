package br.com.corely.comercial.billingschedule;

import br.com.corely.comercial.billingschedule.dto.BillingFrequencyDto;
import br.com.corely.comercial.billingschedule.dto.BillingScheduleRequest;
import br.com.corely.comercial.billingschedule.dto.BillingScheduleResponse;
import br.com.corely.comercial.studentplan.StudentPlan;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
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

    @Transactional
    public void deactivateSchedule(StudentPlan studentPlan) {
        billingScheduleRepository.findByStudentPlanId(studentPlan.getId())
                .ifPresent(schedule -> {
                    schedule.setActive(false);
                    billingScheduleRepository.save(schedule);
                });
    }

    @Transactional
    public void renewSchedule(StudentPlan studentPlan) {
        var existing = billingScheduleRepository.findByStudentPlanId(studentPlan.getId());
        if (existing.isPresent()) {
            var schedule = existing.get();
            schedule.setActive(true);
            schedule.setNextBillingDate(calculateNextBillingDate(
                    studentPlan.getEndDate(), schedule.getFrequency(), schedule.getBillingDay()));
            billingScheduleRepository.save(schedule);
        } else {
            createSchedule(studentPlan, studentPlan.getStartDate().getDayOfMonth());
        }
    }

    public BillingSchedule createSchedule(StudentPlan studentPlan, Integer billingDay) {
        var schedule = new BillingSchedule();
        schedule.setStudio(studentPlan.getStudio());
        schedule.setStudentPlan(studentPlan);
        schedule.setFrequency(BillingFrequency.MONTHLY);
        schedule.setBillingDay(billingDay);
        schedule.setNextBillingDate(calculateNextBillingDate(studentPlan.getStartDate(), BillingFrequency.MONTHLY, billingDay));
        schedule.setActive(true);
        return billingScheduleRepository.save(schedule);
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
            case WEEKLY, MONTHLY -> applyBillingDay(startDate, billingDay);
            case BIWEEKLY -> applyBillingDay(startDate.plusWeeks(2), billingDay);
            case QUARTERLY -> billingDayInMonth(startDate.plusMonths(3), billingDay);
            case SEMIANNUAL -> billingDayInMonth(startDate.plusMonths(6), billingDay);
            case ANNUAL -> billingDayInMonth(startDate.plusYears(1), billingDay);
        };
    }

    private static LocalDate applyBillingDay(LocalDate startDate, int billingDay) {
        int clamped = Math.min(billingDay, startDate.lengthOfMonth());
        var candidate = startDate.withDayOfMonth(clamped);
        if (candidate.isBefore(startDate)) {
            candidate = candidate.plusMonths(1);
            int reclamped = Math.min(billingDay, candidate.lengthOfMonth());
            candidate = candidate.withDayOfMonth(reclamped);
        }
        return candidate;
    }

    private static LocalDate billingDayInMonth(LocalDate periodDate, int billingDay) {
        int clamped = Math.min(billingDay, periodDate.lengthOfMonth());
        return periodDate.withDayOfMonth(clamped);
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
