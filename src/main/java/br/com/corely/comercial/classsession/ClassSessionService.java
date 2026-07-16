package br.com.corely.comercial.classsession;

import br.com.corely.comercial.classsession.dto.ClassSessionRequest;
import br.com.corely.comercial.classsession.dto.ClassSessionResponse;
import br.com.corely.comercial.classsession.dto.SessionStatusDto;
import br.com.corely.comercial.scheduleslot.ScheduleSlotRepository;
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

@Service("comercialClassSessionService")
@RequiredArgsConstructor
public class ClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public ClassSessionResponse create(ClassSessionRequest request) {
        var slot = scheduleSlotRepository.findById(request.getScheduleSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleSlot not found"));

        validateSlotActive(slot);
        validateDuplicate(request.getScheduleSlotId(), request.getSessionDate(), null);
        validateTimeRange(request.getStartTime(), request.getEndTime());

        var session = new ClassSession();
        session.setScheduleSlot(slot);
        session.setStudio(studioRepository.getReferenceById(tenantContext.getCurrentStudioId()));
        session.setSessionDate(request.getSessionDate());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setCapacity(slot.getCapacity());
        session.setBookedCount(0);
        session.setActive(request.getActive() != null ? request.getActive() : true);

        if (request.getStatus() != null) {
            session.setStatus(SessionStatus.valueOf(request.getStatus().name()));
        }

        session = classSessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public Page<ClassSessionResponse> findAll(UUID scheduleSlotId, SessionStatusDto status, Pageable pageable) {
        if (scheduleSlotId != null) {
            return classSessionRepository.findByScheduleSlotId(scheduleSlotId, pageable)
                    .map(this::toResponse);
        }
        if (status != null) {
            return classSessionRepository.findByStatus(SessionStatus.valueOf(status.name()), pageable)
                    .map(this::toResponse);
        }
        return classSessionRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ClassSessionResponse findById(UUID id) {
        var session = classSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));
        return toResponse(session);
    }

    @Transactional
    public ClassSessionResponse startSession(UUID id) {
        var session = classSessionRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        if (!session.getActive()) {
            throw new BusinessException("Cannot start an inactive session");
        }

        session.start();
        session = classSessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional
    public ClassSessionResponse finishSession(UUID id) {
        var session = classSessionRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        session.finish();
        session = classSessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional
    public ClassSessionResponse update(UUID id, ClassSessionRequest request) {
        var session = classSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        if (!session.isScheduled()) {
            throw new BusinessException("Cannot modify a session with status " + session.getStatus());
        }

        validateDuplicate(request.getScheduleSlotId(), request.getSessionDate(), id);
        validateTimeRange(request.getStartTime(), request.getEndTime());

        var slot = scheduleSlotRepository.findById(request.getScheduleSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("ScheduleSlot not found"));

        validateSlotActive(slot);

        session.setScheduleSlot(slot);
        session.setSessionDate(request.getSessionDate());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());

        if (request.getStatus() != null) {
            session.setStatus(SessionStatus.valueOf(request.getStatus().name()));
        }
        if (request.getActive() != null) {
            session.setActive(request.getActive());
        }

        session = classSessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional
    public void delete(UUID id) {
        var session = classSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));
        if (session.getActive()) {
            session.setActive(false);
            classSessionRepository.save(session);
        }
    }

    private void validateSlotActive(br.com.corely.comercial.scheduleslot.ScheduleSlot slot) {
        if (!slot.getActive()) {
            throw new BusinessException("Cannot use an inactive schedule slot");
        }
    }

    private void validateDuplicate(UUID scheduleSlotId, java.time.LocalDate sessionDate, UUID excludeId) {
        boolean duplicate = classSessionRepository.existsDuplicate(scheduleSlotId, sessionDate, excludeId);
        if (duplicate) {
            throw new BusinessException("A session already exists for this schedule slot on the same date");
        }
    }

    private void validateTimeRange(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("endTime must be greater than startTime");
        }
    }

    private ClassSessionResponse toResponse(ClassSession session) {
        return new ClassSessionResponse(
                session.getId(),
                session.getScheduleSlot().getId(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getCapacity(),
                session.getBookedCount(),
                SessionStatusDto.valueOf(session.getStatus().name()),
                session.getActive(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
