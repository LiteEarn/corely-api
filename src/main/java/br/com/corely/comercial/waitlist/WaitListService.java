package br.com.corely.comercial.waitlist;

import br.com.corely.comercial.booking.BookingRepository;
import br.com.corely.comercial.classsession.ClassSessionRepository;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.comercial.waitlist.dto.WaitListRequest;
import br.com.corely.comercial.waitlist.dto.WaitListResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service("comercialWaitListService")
@RequiredArgsConstructor
public class WaitListService {

    private final WaitListRepository waitListRepository;
    private final ClassSessionRepository classSessionRepository;
    private final StudentRepository studentRepository;
    private final StudioRepository studioRepository;
    private final BookingRepository bookingRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public WaitListResponse create(WaitListRequest request) {
        var session = classSessionRepository.findById(request.getClassSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        var student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!session.getActive()) {
            throw new BusinessException("Cannot add to wait list for an inactive session");
        }

        boolean alreadyWaiting = waitListRepository.existsActiveWaiting(request.getClassSessionId(), request.getStudentId());
        if (alreadyWaiting) {
            throw new BusinessException("Student is already on the wait list for this session");
        }

        boolean hasConfirmedBooking = bookingRepository.existsByClassSessionIdAndStudentId(request.getClassSessionId(), request.getStudentId());
        if (hasConfirmedBooking) {
            throw new BusinessException("Student already has a confirmed booking for this session");
        }

        int nextPosition = waitListRepository.findMaxPositionByClassSessionId(request.getClassSessionId()) + 1;

        var entry = new WaitList();
        entry.setClassSession(session);
        entry.setStudent(student);
        entry.setPosition(nextPosition);
        entry.setStudio(studioRepository.getReferenceById(tenantContext.getCurrentStudioId()));

        entry = waitListRepository.save(entry);
        return toResponse(entry);
    }

    @Transactional(readOnly = true)
    public Page<WaitListResponse> findAll(UUID classSessionId, Pageable pageable) {
        if (classSessionId != null) {
            return waitListRepository.findActiveByClassSessionId(classSessionId, pageable)
                    .map(this::toResponse);
        }
        return waitListRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WaitListResponse findById(UUID id) {
        var entry = waitListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WaitList entry not found"));
        return toResponse(entry);
    }

    @Transactional
    public void cancel(UUID id) {
        var entry = waitListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WaitList entry not found"));

        if (entry.getStatus() != WaitListStatus.WAITING) {
            throw new BusinessException("Cannot cancel a wait list entry with status " + entry.getStatus());
        }

        entry.setStatus(WaitListStatus.CANCELLED);
        entry.setActive(false);
        waitListRepository.save(entry);
    }

    private WaitListResponse toResponse(WaitList entry) {
        return new WaitListResponse(
                entry.getId(),
                entry.getClassSession().getId(),
                entry.getStudent().getId(),
                entry.getPosition(),
                entry.getStatus(),
                entry.getActive(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
