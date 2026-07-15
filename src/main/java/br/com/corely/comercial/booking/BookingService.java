package br.com.corely.comercial.booking;

import br.com.corely.comercial.booking.dto.BookingRequest;
import br.com.corely.comercial.booking.dto.BookingResponse;
import br.com.corely.comercial.classsession.ClassSession;
import br.com.corely.comercial.classsession.ClassSessionRepository;
import br.com.corely.comercial.classsession.SessionStatus;
import br.com.corely.comercial.studentplan.StudentPlanRepository;
import br.com.corely.comercial.studentplan.StudentPlanStatus;
import br.com.corely.comercial.tenant.ComercialTenantContext;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service("comercialBookingService")
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ClassSessionRepository classSessionRepository;
    private final StudentRepository studentRepository;
    private final StudentPlanRepository studentPlanRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public BookingResponse create(BookingRequest request) {
        var classSession = classSessionRepository.findByIdWithLock(request.getClassSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        validateClassSessionAvailable(classSession);

        var student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        validateStudentActive(student);

        var sessionDate = classSession.getSessionDate();

        var studentPlan = studentPlanRepository.findActiveByStudentIdAndDate(
                        student.getId(), StudentPlanStatus.ACTIVE, sessionDate)
                .orElseThrow(() -> new BusinessException("Student does not have an active plan for this session date"));

        if (studentPlan.getBookingBlocked()) {
            throw new BusinessException("Student has booking blocked");
        }

        if (bookingRepository.existsByClassSessionIdAndStudentId(classSession.getId(), student.getId())) {
            throw new BusinessException("Student already has a booking for this class session");
        }

        var booking = new Booking();
        booking.setStudio(studioRepository.getReferenceById(tenantContext.getCurrentStudioId()));
        booking.setClassSession(classSession);
        booking.setStudent(student);
        booking.setBookingDateTime(LocalDateTime.now());

        booking = bookingRepository.save(booking);

        classSession.setBookedCount(classSession.getBookedCount() + 1);
        classSessionRepository.save(classSession);

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> findAll(UUID classSessionId, UUID studentId, BookingStatus status, Pageable pageable) {
        if (classSessionId != null) {
            return bookingRepository.findByClassSessionId(classSessionId, pageable)
                    .map(this::toResponse);
        }
        if (studentId != null) {
            return bookingRepository.findByStudentId(studentId, pageable)
                    .map(this::toResponse);
        }
        if (status != null) {
            return bookingRepository.findByStatus(status, pageable)
                    .map(this::toResponse);
        }
        return bookingRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(UUID id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return toResponse(booking);
    }

    @Transactional
    public void delete(UUID id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getActive()) {
            return;
        }

        booking.setActive(false);
        booking.setStatus(BookingStatus.CANCELLED);

        var classSession = booking.getClassSession();
        if (classSession.getBookedCount() > 0) {
            classSession.setBookedCount(classSession.getBookedCount() - 1);
            classSessionRepository.save(classSession);
        }

        bookingRepository.save(booking);
    }

    private void validateClassSessionAvailable(ClassSession classSession) {
        if (!classSession.getActive()) {
            throw new BusinessException("ClassSession is not active");
        }
        if (classSession.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessException("ClassSession status must be SCHEDULED");
        }
        if (classSession.getBookedCount() >= classSession.getCapacity()) {
            throw new BusinessException("ClassSession is full");
        }
    }

    private void validateStudentActive(Student student) {
        if (student.getActive() == null || !student.getActive()) {
            throw new BusinessException("Student is not active");
        }
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getClassSession().getId(),
                booking.getStudent().getId(),
                booking.getStudent().getFullName(),
                booking.getBookingDateTime(),
                booking.getStatus(),
                booking.getActive(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
