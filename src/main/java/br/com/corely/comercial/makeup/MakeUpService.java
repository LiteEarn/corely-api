package br.com.corely.comercial.makeup;

import br.com.corely.comercial.attendance.AttendanceRepository;
import br.com.corely.comercial.attendance.AttendanceStatus;
import br.com.corely.comercial.booking.BookingRepository;
import br.com.corely.comercial.booking.BookingService;
import br.com.corely.comercial.booking.dto.BookingRequest;
import br.com.corely.comercial.classsession.ClassSession;
import br.com.corely.comercial.classsession.ClassSessionFinishedEvent;
import br.com.corely.comercial.classsession.ClassSessionRepository;
import br.com.corely.comercial.makeup.dto.MakeUpCreditRequest;
import br.com.corely.comercial.makeup.dto.MakeUpCreditResponse;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service("comercialMakeUpService")
@RequiredArgsConstructor
public class MakeUpService {

    private final MakeUpRepository makeUpRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassSessionRepository classSessionRepository;
    private final BookingRepository bookingRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;
    private final BookingService bookingService;
    private final MakeUpProperties makeUpProperties;

    @Transactional(readOnly = true)
    public Page<MakeUpCreditResponse> findAll(UUID studentId, MakeUpCreditStatus status, Pageable pageable) {
        if (studentId != null) {
            return makeUpRepository.findByStudentId(studentId, pageable).map(this::toResponse);
        }
        if (status != null) {
            return makeUpRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        return makeUpRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MakeUpCreditResponse findById(UUID id) {
        var credit = makeUpRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MakeUpCredit not found"));
        return toResponse(credit);
    }

    @Transactional
    public MakeUpCreditResponse use(UUID creditId, MakeUpCreditRequest request) {
        var credit = makeUpRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("MakeUpCredit not found"));

        validateCreditAvailable(credit);

        var classSession = classSessionRepository.findByIdWithLock(request.getClassSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        validateSessionAvailableForMakeUp(classSession);
        validateNotAlreadyStarted(classSession);

        var bookingRequest = new BookingRequest();
        bookingRequest.setClassSessionId(request.getClassSessionId());
        bookingRequest.setStudentId(credit.getStudent().getId());

        var bookingResponse = bookingService.create(bookingRequest);

        var booking = bookingRepository.findById(bookingResponse.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        credit.setMakeUpBooking(booking);
        credit.setStatus(MakeUpCreditStatus.USED);
        makeUpRepository.save(credit);

        return toResponse(credit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClassSessionFinished(ClassSessionFinishedEvent event) {
        var attendances = attendanceRepository
                .findByClassSessionId(event.classSessionId(), Pageable.unpaged())
                .getContent();

        for (var attendance : attendances) {
            if (attendance.getStatus() == AttendanceStatus.PRESENT || attendance.getStatus() == AttendanceStatus.EXCUSED) {
                continue;
            }

            var booking = attendance.getBooking();
            var classSession = booking.getClassSession();

            var credit = new MakeUpCredit();
            credit.setStudio(studioRepository.getReferenceById(classSession.getStudio().getId()));
            credit.setStudent(booking.getStudent());
            credit.setOriginalAttendance(attendance);
            credit.setOriginalClassSession(classSession);
            credit.setExpirationDate(LocalDate.now().plusDays(makeUpProperties.getExpirationDays()));
            credit.setStatus(MakeUpCreditStatus.AVAILABLE);
            credit.setActive(true);

            makeUpRepository.save(credit);
        }
    }

    private void validateCreditAvailable(MakeUpCredit credit) {
        if (!credit.getActive()) {
            throw new BusinessException("MakeUpCredit is not active");
        }
        if (credit.getStatus() == MakeUpCreditStatus.EXPIRED) {
            throw new BusinessException("MakeUpCredit has expired");
        }
        if (credit.getStatus() == MakeUpCreditStatus.CANCELLED) {
            throw new BusinessException("MakeUpCredit has been cancelled");
        }
        if (credit.getStatus() == MakeUpCreditStatus.USED) {
            throw new BusinessException("MakeUpCredit has already been used");
        }
        if (credit.getExpirationDate().isBefore(LocalDate.now())) {
            credit.setStatus(MakeUpCreditStatus.EXPIRED);
            makeUpRepository.save(credit);
            throw new BusinessException("MakeUpCredit has expired");
        }
    }

    private void validateSessionAvailableForMakeUp(ClassSession classSession) {
        if (!classSession.getActive()) {
            throw new BusinessException("ClassSession is not active");
        }
        if (classSession.isFinished() || classSession.isCancelled()) {
            throw new BusinessException("Cannot use make-up credit for a finished or cancelled session");
        }
    }

    private void validateNotAlreadyStarted(ClassSession classSession) {
        var sessionStart = LocalDateTime.of(classSession.getSessionDate(), classSession.getStartTime());
        if (LocalDateTime.now().isAfter(sessionStart) || LocalDateTime.now().isEqual(sessionStart)) {
            throw new BusinessException("Cannot use make-up credit for a session that has already started");
        }
    }

    private MakeUpCreditResponse toResponse(MakeUpCredit credit) {
        return new MakeUpCreditResponse(
                credit.getId(),
                credit.getStudent().getId(),
                credit.getStudent().getFullName(),
                credit.getOriginalAttendance().getId(),
                credit.getOriginalClassSession().getId(),
                credit.getMakeUpBooking() != null ? credit.getMakeUpBooking().getId() : null,
                credit.getExpirationDate(),
                credit.getStatus(),
                credit.getReason(),
                credit.getActive(),
                credit.getCreatedAt(),
                credit.getUpdatedAt()
        );
    }
}
