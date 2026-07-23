package br.com.corely.comercial.scheduleslot;

import br.com.corely.comercial.schedule.ScheduleRepository;
import br.com.corely.comercial.scheduleslot.dto.ScheduleSlotRequest;
import br.com.corely.comercial.scheduleslot.dto.ScheduleSlotResponse;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleSlotService {

    private final ScheduleSlotRepository scheduleSlotRepository;
    private final ScheduleRepository scheduleRepository;
    private final StudioRepository studioRepository;
    private final InstructorRepository instructorRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public ScheduleSlotResponse create(UUID scheduleId, ScheduleSlotRequest request) {
        var schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        validateScheduleActive(schedule);
        validateTimeRange(request);
        validateNoOverlap(scheduleId, request, null);

        var slot = new ScheduleSlot();
        slot.setSchedule(schedule);
        slot.setStudio(studioRepository.getReferenceById(tenantContext.getCurrentStudioId()));
        slot.setDayOfWeek(request.getDayOfWeek());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setCapacity(request.getCapacity());
        slot.setActive(request.getActive() != null ? request.getActive() : true);
        if (request.getInstructorId() != null) {
            slot.setInstructor(instructorRepository.getReferenceById(request.getInstructorId()));
        }
        slot.setRoomId(request.getRoomId());

        slot = scheduleSlotRepository.save(slot);
        return toResponse(slot);
    }

    @Transactional(readOnly = true)
    public List<ScheduleSlotResponse> findByScheduleId(UUID scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Schedule not found");
        }
        return scheduleSlotRepository.findByScheduleIdOrderByDayOfWeekAscStartTimeAsc(scheduleId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleSlotResponse findById(UUID id) {
        var slot = scheduleSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleSlot not found"));
        return toResponse(slot);
    }

    @Transactional
    public ScheduleSlotResponse update(UUID id, ScheduleSlotRequest request) {
        var slot = scheduleSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleSlot not found"));

        validateScheduleActive(slot.getSchedule());
        validateTimeRange(request);
        validateNoOverlap(slot.getSchedule().getId(), request, id);

        slot.setDayOfWeek(request.getDayOfWeek());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setCapacity(request.getCapacity());
        if (request.getActive() != null) {
            slot.setActive(request.getActive());
        }
        if (request.getInstructorId() != null) {
            slot.setInstructor(instructorRepository.getReferenceById(request.getInstructorId()));
        } else {
            slot.setInstructor(null);
        }
        slot.setRoomId(request.getRoomId());

        slot = scheduleSlotRepository.save(slot);
        return toResponse(slot);
    }

    @Transactional
    public void delete(UUID id) {
        var slot = scheduleSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleSlot not found"));
        if (slot.getActive()) {
            slot.setActive(false);
            scheduleSlotRepository.save(slot);
        }
    }

    private void validateScheduleActive(br.com.corely.comercial.schedule.Schedule schedule) {
        if (!schedule.getActive()) {
            throw new BusinessException("Cannot modify slots on an inactive schedule");
        }
    }

    private void validateTimeRange(ScheduleSlotRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("endTime must be greater than startTime");
        }
    }

    private void validateNoOverlap(UUID scheduleId, ScheduleSlotRequest request, UUID excludeId) {
        boolean overlap = scheduleSlotRepository.existsOverlappingSlot(
                scheduleId, request.getDayOfWeek(),
                request.getStartTime(), request.getEndTime(), excludeId);
        if (overlap) {
            throw new BusinessException("Schedule slot overlaps with an existing slot on the same day");
        }
    }

    private ScheduleSlotResponse toResponse(ScheduleSlot slot) {
        var instructor = slot.getInstructor();
        return new ScheduleSlotResponse(
                slot.getId(),
                slot.getSchedule().getId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getCapacity(),
                slot.getActive(),
                instructor != null ? instructor.getId() : null,
                instructor != null ? instructor.getFullName() : null,
                slot.getRoomId(),
                slot.getCreatedAt(),
                slot.getUpdatedAt()
        );
    }
}
