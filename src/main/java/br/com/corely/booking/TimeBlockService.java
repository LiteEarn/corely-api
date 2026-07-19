package br.com.corely.booking;

import br.com.corely.booking.dto.TimeBlockRequest;
import br.com.corely.booking.dto.TimeBlockResponse;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeBlockService {

    private final TimeBlockRepository timeBlockRepository;
    private final StudioRepository studioRepository;

    @Transactional
    public TimeBlockResponse create(TimeBlockRequest request) {
        var studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        var block = new TimeBlock();
        block.setStudio(studio);
        block.setInstructorId(request.getInstructorId());
        block.setRoomId(request.getRoomId());
        block.setBlockType(request.getBlockType());
        block.setDescription(request.getDescription());
        block.setStartDate(request.getStartDate());
        block.setEndDate(request.getEndDate());

        block = timeBlockRepository.save(block);
        return toResponse(block);
    }

    @Transactional(readOnly = true)
    public List<TimeBlockResponse> findActiveBlocks(UUID studioId, LocalDateTime startDate, LocalDateTime endDate) {
        return timeBlockRepository.findActiveBlocks(studioId, startDate, endDate)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TimeBlockResponse findById(UUID id) {
        var block = timeBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeBlock not found"));
        return toResponse(block);
    }

    @Transactional
    public void delete(UUID id) {
        var block = timeBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeBlock not found"));
        timeBlockRepository.delete(block);
    }

    @Transactional(readOnly = true)
    public boolean isTimeBlocked(UUID studioId, Long instructorId, Long roomId,
                                  LocalDateTime startDate, LocalDateTime endDate) {
        var blocks = timeBlockRepository.findActiveBlocks(studioId, startDate, endDate);
        for (var block : blocks) {
            if (block.getInstructorId() != null && block.getInstructorId().equals(instructorId)) {
                return true;
            }
            if (block.getRoomId() != null && block.getRoomId().equals(roomId)) {
                return true;
            }
        }
        return false;
    }

    private TimeBlockResponse toResponse(TimeBlock block) {
        return new TimeBlockResponse(
                block.getId(),
                block.getStudio().getId(),
                block.getInstructorId(),
                block.getRoomId(),
                block.getBlockType(),
                block.getDescription(),
                block.getStartDate(),
                block.getEndDate(),
                block.getCreatedAt(),
                block.getUpdatedAt()
        );
    }
}
