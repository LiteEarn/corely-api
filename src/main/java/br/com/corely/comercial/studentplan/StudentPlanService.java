package br.com.corely.comercial.studentplan;

import br.com.corely.comercial.billingschedule.BillingSchedule;
import br.com.corely.comercial.billingschedule.BillingScheduleRepository;
import br.com.corely.comercial.contractsnapshot.ContractSnapshotService;
import br.com.corely.comercial.studentplan.dto.StudentPlanRequest;
import br.com.corely.comercial.studentplan.dto.StudentPlanResponse;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.StudioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentPlanService {

    private final StudentPlanRepository studentPlanRepository;
    private final StudentRepository studentRepository;
    private final StudioRepository studioRepository;
    private final ContractSnapshotService contractSnapshotService;
    private final ComercialTenantContext tenantContext;
    private final BillingScheduleRepository billingScheduleRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public StudentPlanResponse create(StudentPlanRequest request) {
        return doCreate(request).response();
    }

    @Transactional
    public StudentPlanData createWithEntity(StudentPlanRequest request) {
        return doCreate(request);
    }

    private StudentPlanData doCreate(StudentPlanRequest request) {
        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (studentPlanRepository.existsByStudentIdAndStatus(request.getStudentId(), StudentPlanStatus.ACTIVE)) {
            throw new BusinessException("Student already has an active plan.");
        }

        var snapshot = contractSnapshotService.create(request.getPlanId());

        var enrollment = new StudentPlan();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setContractSnapshot(snapshot);
        enrollment.setStartDate(request.getStartDate());
        enrollment.setEndDate(request.getEndDate());
        enrollment.setStatus(StudentPlanStatus.ACTIVE);

        enrollment = studentPlanRepository.save(enrollment);

        return new StudentPlanData(enrollment, toResponse(enrollment));
    }

    @Transactional
    public StudentPlanResponse cancel(UUID id) {
        return transitionStatus(id, StudentPlanStatus.ACTIVE, StudentPlanStatus.CANCELLED);
    }

    @Transactional
    public StudentPlanResponse suspend(UUID id) {
        var enrollment = studentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudentPlan not found"));

        if (enrollment.getStatus() != StudentPlanStatus.ACTIVE) {
            throw new BusinessException("StudentPlan must be ACTIVE to be suspended");
        }

        if (studentPlanRepository.existsByStudentIdAndStatus(enrollment.getStudent().getId(), StudentPlanStatus.SUSPENDED)) {
            throw new BusinessException("Student already has a suspended plan.");
        }

        enrollment.setStatus(StudentPlanStatus.SUSPENDED);
        enrollment.setSuspensionReason(SuspensionReason.MANUAL);
        if (enrollment.getCancellationDate() == null) {
            enrollment.setCancellationDate(LocalDate.now());
        }

        enrollment = studentPlanRepository.save(enrollment);
        return toResponse(enrollment);
    }

    @Transactional
    public StudentPlanResponse reactivate(UUID id) {
        return transitionStatus(id, StudentPlanStatus.SUSPENDED, StudentPlanStatus.ACTIVE);
    }

    private StudentPlanResponse transitionStatus(UUID id, StudentPlanStatus from, StudentPlanStatus to) {
        var enrollment = studentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudentPlan not found"));

        if (enrollment.getStatus() != from) {
            throw new BusinessException("StudentPlan must be " + from + " to transition to " + to);
        }

        if ((to == StudentPlanStatus.CANCELLED || to == StudentPlanStatus.SUSPENDED)
                && enrollment.getCancellationDate() == null) {
            enrollment.setCancellationDate(LocalDate.now());
        }

        if (to == StudentPlanStatus.ACTIVE) {
            enrollment.setCancellationDate(null);
            enrollment.setCancellationReason(null);
            enrollment.setSuspensionReason(null);
        }

        if (studentPlanRepository.existsByStudentIdAndStatus(enrollment.getStudent().getId(), to)) {
            throw new BusinessException("Student already has a plan with status " + to);
        }

        enrollment.setStatus(to);
        enrollment = studentPlanRepository.save(enrollment);
        return toResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public StudentPlanResponse findById(UUID id) {
        var enrollment = studentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudentPlan not found"));
        var billingSchedule = billingScheduleRepository.findByStudentPlanId(id).orElse(null);
        return toResponse(enrollment, billingSchedule);
    }

    @Transactional(readOnly = true)
    public List<StudentPlanResponse> findAll() {
        var enrollments = studentPlanRepository.findAll();
        var studentPlanIds = enrollments.stream().map(StudentPlan::getId).toList();
        var billingSchedules = billingScheduleRepository.findByStudentPlanIdIn(studentPlanIds);
        var billingScheduleMap = new HashMap<UUID, BillingSchedule>();
        billingSchedules.forEach(bs -> billingScheduleMap.put(bs.getStudentPlan().getId(), bs));

        return enrollments.stream()
                .map(sp -> toResponse(sp, billingScheduleMap.get(sp.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentPlan findEntityById(UUID id) {
        return studentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudentPlan not found"));
    }

    @Transactional(readOnly = true)
    public StudentPlanResponse findActiveByStudent(UUID studentId) {
        return studentPlanRepository.findByStudentIdAndStatus(studentId, StudentPlanStatus.ACTIVE)
                .map(sp -> {
                    var billingSchedule = billingScheduleRepository.findByStudentPlanId(sp.getId()).orElse(null);
                    return toResponse(sp, billingSchedule);
                })
                .orElse(null);
    }

    public record StudentPlanData(StudentPlan entity, StudentPlanResponse response) {}

    private StudentPlanResponse toResponse(StudentPlan enrollment) {
        return toResponse(enrollment, null);
    }

    private StudentPlanResponse toResponse(StudentPlan enrollment, BillingSchedule billingSchedule) {
        var snapshot = enrollment.getContractSnapshot();

        var response = new StudentPlanResponse();
        response.setId(enrollment.getId());
        response.setStudentId(enrollment.getStudent().getId());
        response.setStudentName(enrollment.getStudent().getFullName());
        response.setContractSnapshotId(snapshot.getId());
        response.setSnapshotName(snapshot.getPlanName());
        response.setPlanId(snapshot.getPlanId());
        response.setPlanDescription(snapshot.getPlanDescription());
        response.setPlanPrice(snapshot.getPlanPrice());
        response.setWeeklyClasses(extractWeeklyClasses(snapshot.getRules()));
        response.setStatus(enrollment.getStatus());
        response.setStartDate(enrollment.getStartDate());
        response.setEndDate(enrollment.getEndDate());
        response.setCancellationDate(enrollment.getCancellationDate());
        response.setCancellationReason(enrollment.getCancellationReason());
        response.setBookingBlocked(enrollment.getBookingBlocked());
        response.setSuspensionReason(enrollment.getSuspensionReason());
        response.setCreatedAt(enrollment.getCreatedAt());
        response.setUpdatedAt(enrollment.getUpdatedAt());

        if (billingSchedule != null && billingSchedule.getActive()) {
            response.setNextBillingDate(billingSchedule.getNextBillingDate());
            response.setNextBillingFrequency(billingSchedule.getFrequency());
            response.setNextBillingDay(billingSchedule.getBillingDay());
            response.setNextBillingActive(billingSchedule.getActive());
            response.setBillingCycle(billingSchedule.getFrequency());
        } else {
            response.setBillingCycle(null);
            response.setNextBillingDate(null);
            response.setNextBillingFrequency(null);
            response.setNextBillingDay(null);
            response.setNextBillingActive(null);
        }

        return response;
    }

    private Integer extractWeeklyClasses(String rulesJson) {
        if (rulesJson == null || rulesJson.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> rules = objectMapper.readValue(
                    rulesJson, new TypeReference<>() {});
            Object value = rules.get("WEEKLY_CLASSES");
            if (value == null) return null;
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            log.debug("Failed to parse rules JSON for weeklyClasses: {}", e.getMessage());
            return null;
        }
    }
}
