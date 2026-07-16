package br.com.corely.comercial.attendance;

import br.com.corely.comercial.attendance.dto.AttendanceRequest;
import br.com.corely.comercial.attendance.dto.AttendanceResponse;
import br.com.corely.comercial.attendance.dto.BulkAttendanceRequest;
import br.com.corely.comercial.attendance.dto.BulkAttendanceResponse;
import br.com.corely.comercial.booking.Booking;
import br.com.corely.comercial.booking.BookingRepository;
import br.com.corely.comercial.classsession.ClassSession;
import br.com.corely.comercial.classsession.ClassSessionRepository;
import br.com.corely.comercial.classsession.SessionStatus;
import br.com.corely.comercial.tenant.ComercialTenantContext;
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

@Service("comercialAttendanceService")
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassSessionRepository classSessionRepository;
    private final BookingRepository bookingRepository;
    private final StudioRepository studioRepository;
    private final ComercialTenantContext tenantContext;

    @Transactional
    public AttendanceResponse register(UUID sessionId, AttendanceRequest request) {
        var classSession = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        if (classSession.getStatus() == SessionStatus.FINISHED || classSession.getStatus() == SessionStatus.CANCELLED) {
            throw new BusinessException("Attendance cannot be registered for a finished or cancelled session");
        }

        validateSessionAlreadyStarted(classSession);

        var booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getActive()) {
            throw new BusinessException("Booking is not active");
        }

        if (!booking.getClassSession().getId().equals(sessionId)) {
            throw new BusinessException("Booking does not belong to this class session");
        }

        var attendance = attendanceRepository
                .findByClassSessionIdAndBookingId(sessionId, booking.getId())
                .orElse(null);

        if (attendance == null) {
            attendance = new Attendance();
            attendance.setStudio(studioRepository.getReferenceById(tenantContext.getCurrentStudioId()));
            attendance.setBooking(booking);
            attendance.setActive(true);
        }

        attendance.setStatus(request.getStatus());
        attendance.setNotes(request.getNotes());
        if (request.getStatus() == AttendanceStatus.PRESENT && attendance.getCheckedInAt() == null) {
            attendance.setCheckedInAt(LocalDateTime.now());
        }

        attendance = attendanceRepository.save(attendance);
        return toResponse(attendance);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> findBySessionId(UUID sessionId, Pageable pageable) {
        if (!classSessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("ClassSession not found");
        }
        return attendanceRepository.findByClassSessionId(sessionId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> findByBookingId(UUID bookingId, Pageable pageable) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new ResourceNotFoundException("Booking not found");
        }
        return attendanceRepository.findByBookingId(bookingId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> findByStudentId(UUID studentId, Pageable pageable) {
        return attendanceRepository.findByStudentId(studentId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public BulkAttendanceResponse bulkSave(BulkAttendanceRequest request) {
        var classSession = classSessionRepository.findById(request.getClassSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        if (classSession.getStatus() == SessionStatus.FINISHED || classSession.getStatus() == SessionStatus.CANCELLED) {
            throw new BusinessException("Attendance cannot be registered for a finished or cancelled session");
        }

        validateSessionAlreadyStarted(classSession);

        int savedCount = 0;

        for (var item : request.getAttendances()) {
            var booking = bookingRepository.findById(item.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + item.getBookingId()));

            if (!booking.getActive()) {
                throw new BusinessException("Booking is not active: " + item.getBookingId());
            }

            if (!booking.getClassSession().getId().equals(request.getClassSessionId())) {
                throw new BusinessException("Booking does not belong to this class session: " + item.getBookingId());
            }

            var attendance = attendanceRepository
                    .findByClassSessionIdAndBookingId(request.getClassSessionId(), booking.getId())
                    .orElse(null);

            if (attendance == null) {
                attendance = new Attendance();
                attendance.setStudio(studioRepository.getReferenceById(tenantContext.getCurrentStudioId()));
                attendance.setBooking(booking);
                attendance.setActive(true);
            }

            var attStatus = item.isPresent() ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT;
            attendance.setStatus(attStatus);
            attendance.setNotes(item.getNotes());
            if (attStatus == AttendanceStatus.PRESENT && attendance.getCheckedInAt() == null) {
                attendance.setCheckedInAt(LocalDateTime.now());
            }

            attendanceRepository.save(attendance);
            savedCount++;
        }

        return new BulkAttendanceResponse(savedCount + " attendance(s) saved successfully", savedCount);
    }

    private void validateSessionAlreadyStarted(ClassSession classSession) {
        var sessionStart = LocalDateTime.of(classSession.getSessionDate(), classSession.getStartTime());
        if (LocalDateTime.now().isBefore(sessionStart)) {
            throw new BusinessException("Attendance cannot be registered before the class has started");
        }
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getBooking().getClassSession().getId(),
                attendance.getBooking().getId(),
                attendance.getBooking().getStudent().getId(),
                attendance.getBooking().getStudent().getFullName(),
                attendance.getStatus(),
                attendance.getNotes(),
                attendance.getCheckedInAt(),
                attendance.getActive(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }
}
