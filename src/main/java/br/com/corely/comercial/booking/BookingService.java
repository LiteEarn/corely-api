package br.com.corely.comercial.booking;

import br.com.corely.comercial.booking.dto.*;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service("comercialBookingService")
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ClassSessionRepository classSessionRepository;
    private final StudentRepository studentRepository;
    private final StudentPlanRepository studentPlanRepository;
    private final StudioRepository studioRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final ComercialTenantContext tenantContext;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BookingResponse create(BookingRequest request) {
        var classSession = classSessionRepository.findByIdWithLock(request.getClassSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        validateClassSessionAvailable(classSession);

        var student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        validateStudentActive(student);

        var sessionDate = classSession.getSessionDate();

        var studentPlan = studentPlanRepository.findByStudentIdAndStatus(
                        student.getId(), StudentPlanStatus.ACTIVE)
                .filter(sp -> (sp.getStartDate() == null || !sp.getStartDate().isAfter(sessionDate))
                        && (sp.getEndDate() == null || !sp.getEndDate().isBefore(sessionDate)))
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

        var classSession = classSessionRepository.findByIdWithLock(booking.getClassSession().getId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        if (classSession.isFinished()) {
            throw new BusinessException("Cannot cancel a booking for a finished session");
        }

        booking.setActive(false);
        booking.setStatus(BookingStatus.CANCELLED);

        if (classSession.getBookedCount() > 0) {
            classSession.setBookedCount(classSession.getBookedCount() - 1);
            classSessionRepository.save(classSession);
        }

        bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingCancelledEvent(this, classSession.getId()));
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> findByAgenda(LocalDate startDate, LocalDate endDate,
                                               UUID instructorId, Long roomId, UUID studentId,
                                               Pageable pageable) {
        return bookingRepository.findByAgenda(startDate, endDate, instructorId, roomId, studentId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ConflictResponse> findConflicts(UUID bookingId) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        var classSession = booking.getClassSession();
        var slot = classSession.getScheduleSlot();
        var sessionDate = classSession.getSessionDate();
        var startTime = classSession.getStartTime();
        var endTime = classSession.getEndTime();

        List<ConflictResponse> conflicts = new ArrayList<>();

        if (slot.getInstructor() != null) {
            UUID instructorId = slot.getInstructor().getId();
            var instructorConflicts = bookingRepository.findConflictingBookings(
                    instructorId, null, sessionDate, startTime, endTime, bookingId);
            for (var conflictingBooking : instructorConflicts) {
                conflicts.add(new ConflictResponse(
                        ConflictResponse.ConflictType.INSTRUCTOR,
                        conflictingBooking.getId(),
                        "Instructor has a conflicting booking in session " + conflictingBooking.getClassSession().getStartTime()));
            }
        }

        if (slot.getRoomId() != null) {
            Long roomId = slot.getRoomId();
            var roomConflicts = bookingRepository.findConflictingBookings(
                    null, roomId, sessionDate, startTime, endTime, bookingId);
            for (var conflictingBooking : roomConflicts) {
                conflicts.add(new ConflictResponse(
                        ConflictResponse.ConflictType.ROOM,
                        conflictingBooking.getId(),
                        "Room " + roomId + " has a conflicting booking in session " + conflictingBooking.getClassSession().getStartTime()));
            }
        }

        UUID studentId = booking.getStudent().getId();
        var studentConflicts = bookingRepository.findConflictingBookingsForStudent(
                studentId, sessionDate, startTime, endTime, bookingId);
        for (var conflictingBooking : studentConflicts) {
            conflicts.add(new ConflictResponse(
                    ConflictResponse.ConflictType.STUDENT,
                    conflictingBooking.getId(),
                    "Student already has a booking in session " + conflictingBooking.getClassSession().getStartTime()));
        }

        return conflicts;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> findAvailability(LocalDate date, UUID instructorId, Long roomId) {
        var sessions = classSessionRepository.findBySessionDateWithSlotAndSchedule(date);

        if (instructorId != null) {
            sessions = sessions.stream()
                    .filter(s -> s.getScheduleSlot().getInstructor() != null
                            && s.getScheduleSlot().getInstructor().getId().equals(instructorId))
                    .collect(Collectors.toList());
        }
        if (roomId != null) {
            sessions = sessions.stream()
                    .filter(s -> Objects.equals(s.getScheduleSlot().getRoomId(), roomId))
                    .collect(Collectors.toList());
        }

        var startOfDay = date.atStartOfDay();
        var endOfDay = date.atTime(LocalTime.MAX);

        // Get all active time blocks for the period
        List<UUID> instructorIds = sessions.stream()
                .map(s -> s.getScheduleSlot().getInstructor())
                .filter(Objects::nonNull)
                .map(i -> i.getId())
                .distinct()
                .toList();

        List<TimeBlock> timeBlocks = new ArrayList<>();
        if (instructorId != null) {
            timeBlocks.addAll(timeBlockRepository.findActiveOverlapping(instructorId, null, startOfDay, endOfDay));
        }
        if (roomId != null) {
            timeBlocks.addAll(timeBlockRepository.findActiveOverlapping(null, roomId, startOfDay, endOfDay));
        }
        if (instructorId == null && roomId == null) {
            for (var iId : instructorIds) {
                timeBlocks.addAll(timeBlockRepository.findActiveOverlapping(iId, null, startOfDay, endOfDay));
            }
        }

        Set<UUID> blockedInstructorIds = timeBlocks.stream()
                .filter(tb -> tb.getInstructor() != null)
                .map(tb -> tb.getInstructor().getId())
                .collect(Collectors.toSet());
        Set<Long> blockedRoomIds = timeBlocks.stream()
                .filter(tb -> tb.getRoomId() != null)
                .map(TimeBlock::getRoomId)
                .collect(Collectors.toSet());

        List<AvailabilityResponse> result = new ArrayList<>();
        for (var session : sessions) {
            int bookedCount = session.getBookedCount();
            int capacity = session.getCapacity();
            int availableSpots = capacity - bookedCount;
            boolean available = true;
            String reason = null;

            if (!session.getActive() || session.isCancelled()) {
                available = false;
                reason = "CANCELLED";
            } else if (bookedCount >= capacity) {
                available = false;
                reason = "FULL";
            } else if (blockedInstructorIds.contains(session.getScheduleSlot().getInstructor() != null
                    ? session.getScheduleSlot().getInstructor().getId() : null)) {
                available = false;
                reason = "INSTRUCTOR_BLOCKED";
            } else if (blockedRoomIds.contains(session.getScheduleSlot().getRoomId())) {
                available = false;
                reason = "ROOM_BLOCKED";
            }

            result.add(new AvailabilityResponse(
                    session.getId(),
                    session.getStartTime(),
                    session.getEndTime(),
                    capacity,
                    bookedCount,
                    Math.max(0, availableSpots),
                    available,
                    reason
            ));
        }

        return result;
    }

    @Transactional
    public void confirm(UUID id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return;
        }

        if (booking.getStatus() != BookingStatus.CANCELLED) {
            throw new BusinessException("Only CANCELLED bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setActive(true);
        bookingRepository.save(booking);
    }

    @Transactional
    public void cancel(UUID id, CancelBookingRequest request) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Only CONFIRMED bookings can be cancelled");
        }

        var classSession = classSessionRepository.findByIdWithLock(booking.getClassSession().getId())
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        if (classSession.isFinished()) {
            throw new BusinessException("Cannot cancel a booking for a finished session");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setActive(false);
        booking.setCancelReason(request.getReason());
        booking.setCancelDescription(request.getDescription());
        booking.setCancelledBy(tenantContext.getCurrentUserId());
        booking.setCancelledAt(LocalDateTime.now());

        if (classSession.getBookedCount() > 0) {
            classSession.setBookedCount(classSession.getBookedCount() - 1);
            classSessionRepository.save(classSession);
        }

        bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingCancelledEvent(this, classSession.getId()));
    }

    @Transactional
    public void reschedule(UUID id, RescheduleBookingRequest request) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Only CONFIRMED bookings can be rescheduled");
        }

        var originSessionId = booking.getClassSession().getId();
        var targetSessionId = request.getNewClassSessionId();

        // Lock in deterministic order to avoid deadlock
        List<UUID> orderedIds = new ArrayList<>(List.of(originSessionId, targetSessionId));
        Collections.sort(orderedIds);
        boolean originFirst = orderedIds.get(0).equals(originSessionId);

        var originSession = classSessionRepository.findByIdWithLock(originSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Origin ClassSession not found"));
        var targetSession = classSessionRepository.findByIdWithLock(targetSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Target ClassSession not found"));

        if (!originFirst) {
            // Re-read in locks to ensure consistent order
            if (originSession == null) originSession = classSessionRepository.findByIdWithLock(originSessionId).orElseThrow();
            if (targetSession == null) targetSession = classSessionRepository.findByIdWithLock(targetSessionId).orElseThrow();
        }

        if (!targetSession.getActive() || !targetSession.isScheduled()) {
            throw new BusinessException("Target ClassSession must be active and SCHEDULED");
        }

        if (targetSession.getBookedCount() >= targetSession.getCapacity()) {
            throw new BusinessException("Target ClassSession is full");
        }

        // Check student conflicts in target session
        UUID studentId = booking.getStudent().getId();
        var studentConflicts = bookingRepository.findConflictingBookingsForStudent(
                studentId, targetSession.getSessionDate(),
                targetSession.getStartTime(), targetSession.getEndTime(), id);
        if (!studentConflicts.isEmpty()) {
            throw new BusinessException("Student has a time conflict with the target session");
        }

        // Decrement origin
        if (originSession.getBookedCount() > 0) {
            originSession.setBookedCount(originSession.getBookedCount() - 1);
            classSessionRepository.save(originSession);
        }

        // Increment target
        targetSession.setBookedCount(targetSession.getBookedCount() + 1);
        classSessionRepository.save(targetSession);

        // Update booking
        booking.setClassSession(targetSession);
        bookingRepository.save(booking);
    }

    @Transactional
    public void markNoShow(UUID id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Only CONFIRMED bookings can be marked as no-show");
        }

        var classSession = booking.getClassSession();
        if (!classSession.isInProgress() && !classSession.isFinished()) {
            throw new BusinessException("Cannot mark no-show for a session that has not started yet");
        }

        booking.setStatus(BookingStatus.NO_SHOW);
        bookingRepository.save(booking);
    }

    @Transactional
    public void complete(UUID id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Only CONFIRMED bookings can be completed");
        }

        var classSession = booking.getClassSession();
        if (!classSession.isFinished()) {
            throw new BusinessException("Cannot complete a booking for a session that is not finished yet");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public BookingDashboardResponse getDashboard(LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();

        var sessions = classSessionRepository.findBySessionDateWithSlotAndSchedule(queryDate);
        var sessionIds = sessions.stream().map(s -> s.getId()).toList();

        long totalCapacity = sessions.stream()
                .filter(s -> s.getStatus() != SessionStatus.CANCELLED)
                .mapToInt(ClassSession::getCapacity).sum();
        long totalBooked = sessions.stream()
                .filter(s -> s.getStatus() != SessionStatus.CANCELLED)
                .mapToInt(ClassSession::getBookedCount).sum();

        long totalBookings = bookingRepository.countBySessionDate(queryDate);
        long confirmed = bookingRepository.countBySessionDateAndStatus(queryDate, BookingStatus.CONFIRMED);
        long cancelled = bookingRepository.countBySessionDateAndStatus(queryDate, BookingStatus.CANCELLED);
        long noShow = bookingRepository.countBySessionDateAndStatus(queryDate, BookingStatus.NO_SHOW);
        long completed = bookingRepository.countBySessionDateAndStatus(queryDate, BookingStatus.COMPLETED);

        int freeCapacity = (int) (totalCapacity - totalBooked);
        double occupancyRate = totalCapacity > 0 ? (double) totalBooked / totalCapacity * 100.0 : 0.0;

        return new BookingDashboardResponse(
                queryDate, totalBookings, confirmed, cancelled, noShow, completed,
                (int) totalCapacity, (int) totalBooked, freeCapacity, occupancyRate
        );
    }

    private void validateClassSessionAvailable(ClassSession classSession) {
        if (!classSession.getActive()) {
            throw new BusinessException("ClassSession is not active");
        }
        if (!classSession.isScheduled()) {
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
        var classSession = booking.getClassSession();
        var slot = classSession.getScheduleSlot();
        var instructor = slot != null ? slot.getInstructor() : null;

        return new BookingResponse(
                booking.getId(),
                classSession.getId(),
                booking.getStudent().getId(),
                booking.getStudent().getFullName(),
                booking.getBookingDateTime(),
                booking.getStatus(),
                booking.getActive(),
                booking.getCancelReason(),
                booking.getCancelDescription(),
                booking.getCancelledBy(),
                booking.getCancelledAt(),
                classSession.getSessionDate(),
                classSession.getStartTime(),
                classSession.getEndTime(),
                instructor != null ? instructor.getId() : null,
                instructor != null ? instructor.getFullName() : null,
                slot != null ? slot.getRoomId() : null,
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
