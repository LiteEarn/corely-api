package br.com.corely.comercial.waitlist;

import br.com.corely.comercial.booking.BookingCancelledEvent;
import br.com.corely.comercial.booking.BookingService;
import br.com.corely.comercial.booking.dto.BookingRequest;
import br.com.corely.comercial.classsession.ClassSessionRepository;
import br.com.corely.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WaitListPromotionService {

    private final ClassSessionRepository classSessionRepository;
    private final WaitListRepository waitListRepository;
    private final BookingService bookingService;

    @Transactional
    public void promoteIfNeeded(UUID classSessionId) {
        var session = classSessionRepository.findByIdWithLock(classSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        if (session.isFinished() || session.isCancelled()) {
            return;
        }

        var waitingEntries = waitListRepository.findWaitingByClassSessionIdWithLock(classSessionId);

        if (waitingEntries.isEmpty()) {
            return;
        }

        var next = waitingEntries.get(0);

        var bookingRequest = new BookingRequest();
        bookingRequest.setClassSessionId(classSessionId);
        bookingRequest.setStudentId(next.getStudent().getId());
        bookingService.create(bookingRequest);

        next.setStatus(WaitListStatus.PROMOTED);
        next.setActive(false);
        waitListRepository.save(next);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        promoteIfNeeded(event.classSessionId());
    }
}
