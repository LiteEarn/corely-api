package br.com.corely.comercial.booking;

import br.com.corely.comercial.booking.dto.TimeBlockRequest;
import br.com.corely.comercial.booking.dto.TimeBlockResponse;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service("comercialTimeBlockService")
@RequiredArgsConstructor
public class TimeBlockService {

    private final TimeBlockRepository timeBlockRepository;
    private final InstructorRepository instructorRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public TimeBlockResponse create(TimeBlockRequest request) {
        if (request.getInstructorId() == null && request.getRoomId() == null) {
            throw new BusinessException("At least one of instructorId or roomId must be provided");
        }
        if (!request.getEndDateTime().isAfter(request.getStartDateTime())) {
            throw new BusinessException("endDateTime must be after startDateTime");
        }

        var block = new TimeBlock();
        block.setStudio(studioRepository.getReferenceById(tenantContext.getCurrentStudioId()));
        if (request.getInstructorId() != null) {
            block.setInstructor(instructorRepository.getReferenceById(request.getInstructorId()));
        }
        block.setRoomId(request.getRoomId());
        block.setStartDateTime(request.getStartDateTime());
        block.setEndDateTime(request.getEndDateTime());
        block.setReason(request.getReason());
        block.setBlockType(request.getBlockType());

        block = timeBlockRepository.save(block);
        return toResponse(block);
    }

    @Transactional(readOnly = true)
    public Page<TimeBlockResponse> findAll(UUID instructorId, Long roomId,
                                           LocalDateTime startDate, LocalDateTime endDate,
                                           Pageable pageable) {
        return timeBlockRepository.findByFilters(instructorId, roomId, startDate, endDate, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TimeBlockResponse findById(UUID id) {
        var block = timeBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeBlock not found"));
        return toResponse(block);
    }

    @Transactional
    public TimeBlockResponse update(UUID id, TimeBlockRequest request) {
        var block = timeBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeBlock not found"));

        if (block.getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot edit a time block that has already started");
        }

        if (!request.getEndDateTime().isAfter(request.getStartDateTime())) {
            throw new BusinessException("endDateTime must be after startDateTime");
        }

        if (request.getInstructorId() == null && request.getRoomId() == null) {
            throw new BusinessException("At least one of instructorId or roomId must be provided");
        }

        if (request.getInstructorId() != null) {
            block.setInstructor(instructorRepository.getReferenceById(request.getInstructorId()));
        } else {
            block.setInstructor(null);
        }
        block.setRoomId(request.getRoomId());
        block.setStartDateTime(request.getStartDateTime());
        block.setEndDateTime(request.getEndDateTime());
        block.setReason(request.getReason());
        block.setBlockType(request.getBlockType());

        block = timeBlockRepository.save(block);
        return toResponse(block);
    }

    @Transactional
    public void delete(UUID id) {
        var block = timeBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeBlock not found"));
        if (block.getActive()) {
            block.setActive(false);
            timeBlockRepository.save(block);
        }
    }

    private TimeBlockResponse toResponse(TimeBlock block) {
        var instructor = block.getInstructor();
        return new TimeBlockResponse(
                block.getId(),
                instructor != null ? instructor.getId() : null,
                instructor != null ? instructor.getFullName() : null,
                block.getRoomId(),
                block.getStartDateTime(),
                block.getEndDateTime(),
                block.getReason(),
                block.getBlockType(),
                block.getActive()
        );
    }
}
