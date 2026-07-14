package br.com.corely.comercial.schedule;

import br.com.corely.comercial.schedule.dto.ScheduleRequest;
import br.com.corely.comercial.schedule.dto.ScheduleResponse;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public ScheduleResponse create(ScheduleRequest request) {
        validateUniqueName(null, request.getName());

        var studio = studioRepository.getReferenceById(tenantContext.getCurrentStudioId());

        var schedule = new Schedule();
        schedule.setStudio(studio);
        schedule.setName(request.getName());
        schedule.setDescription(request.getDescription());
        schedule.setActive(request.getActive() != null ? request.getActive() : true);

        schedule = scheduleRepository.save(schedule);
        return toResponse(schedule);
    }

    @Transactional(readOnly = true)
    public Page<ScheduleResponse> findAll(String name, Boolean active, Pageable pageable) {
        Page<Schedule> schedules;

        if (name != null && active != null) {
            schedules = scheduleRepository.findByNameContainingIgnoreCaseAndActive(name, active, pageable);
        } else if (name != null) {
            schedules = scheduleRepository.findByNameContainingIgnoreCase(name, pageable);
        } else if (active != null) {
            schedules = scheduleRepository.findByActive(active, pageable);
        } else {
            schedules = scheduleRepository.findAll(pageable);
        }

        return schedules.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ScheduleResponse findById(UUID id) {
        var schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        return toResponse(schedule);
    }

    @Transactional
    public ScheduleResponse update(UUID id, ScheduleRequest request) {
        var schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        validateUniqueName(id, request.getName());

        schedule.setName(request.getName());
        schedule.setDescription(request.getDescription());
        if (request.getActive() != null) {
            schedule.setActive(request.getActive());
        }

        schedule = scheduleRepository.save(schedule);
        return toResponse(schedule);
    }

    @Transactional
    public void delete(UUID id) {
        var schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        if (!schedule.getActive()) {
            throw new BusinessException("Schedule is already inactive");
        }
        schedule.setActive(false);
        scheduleRepository.save(schedule);
    }

    private void validateUniqueName(UUID id, String name) {
        var studioId = tenantContext.getCurrentStudioId();
        boolean exists = (id != null)
                ? scheduleRepository.existsByStudioIdAndNameAndIdNot(studioId, name, id)
                : scheduleRepository.existsByStudioIdAndName(studioId, name);
        if (exists) {
            throw new BusinessException("Schedule name already exists: " + name);
        }
    }

    private ScheduleResponse toResponse(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getName(),
                schedule.getDescription(),
                schedule.getActive(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
