package br.com.corely.booking;

import br.com.corely.booking.dto.*;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TimeBlockService timeBlockService;
    private final StudioRepository studioRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;

    @Transactional
    public BookingResponse create(BookingRequest request) {
        var studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));
        var student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        var instructor = instructorRepository.findById(request.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));

        Booking originalBooking = null;
        if (request.getMakeUpClass() != null && request.getMakeUpClass()
                && request.getOriginalBookingId() != null) {
            originalBooking = bookingRepository.findById(request.getOriginalBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Original booking not found"));
        }

        var booking = new Booking();
        booking.setStudio(studio);
        booking.setStudent(student);
        booking.setInstructor(instructor);
        booking.setRoomId(request.getRoomId());
        booking.setClassType(request.getClassType());
        booking.setStartDateTime(request.getStartDateTime());
        booking.setEndDateTime(request.getEndDateTime());
        booking.setStatus(BookingStatus.SCHEDULED);
        booking.setCapacity(request.getCapacity());
        booking.setActive(true);

        if (request.getMakeUpClass() != null) {
            booking.setMakeUpClass(request.getMakeUpClass());
        }
        if (originalBooking != null) {
            booking.setOriginalBooking(originalBooking);
        }

        validateBooking(booking);

        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse findById(UUID id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findAgenda(UUID studioId, LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.findByStudioAndDateRange(studioId, startDate, endDate)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConflictResponse> findConflicts(UUID bookingId) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return findConflicts(booking).stream()
                .map(c -> new ConflictResponse(c.type, c.description, c.conflictingBookingId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> findAvailability(UUID studioId, UUID instructorId,
                                                        Long roomId, LocalDate date) {
        List<AvailabilityResponse> result = new ArrayList<>();
        LocalDateTime cursor = date.atTime(6, 0);
        LocalDateTime end = date.atTime(22, 0);

        while (cursor.isBefore(end)) {
            LocalDateTime slotEnd = cursor.plusMinutes(30);
            boolean available = true;
            String reason = null;

            if (LocalDateTime.now().isAfter(cursor)) {
                available = false;
                reason = "Horário passado";
            }

            if (available && !bookingRepository.findConflictingByInstructor(instructorId, cursor, slotEnd).isEmpty()) {
                available = false;
                reason = "Instrutor indisponível";
            }

            if (available && roomId != null
                    && !bookingRepository.findConflictingByRoom(roomId, cursor, slotEnd).isEmpty()) {
                available = false;
                reason = "Sala indisponível";
            }

            if (available && timeBlockService.isTimeBlocked(studioId,
                    Long.valueOf(instructorId.toString().hashCode() & 0x7fffffff),
                    roomId, cursor, slotEnd)) {
                available = false;
                reason = "Horário bloqueado";
            }

            result.add(new AvailabilityResponse(cursor, slotEnd, available, reason));
            cursor = slotEnd;
        }

        return result;
    }

    @Transactional
    public BookingResponse confirm(UUID id) {
        var booking = getActiveBooking(id);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(UUID id, CancellationReason reason, String notes) {
        var booking = getActiveBooking(id);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancellationNotes(notes);
        booking.setActive(false);
        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse reschedule(UUID id, LocalDateTime newStart, LocalDateTime newEnd) {
        var booking = getActiveBooking(id);
        booking.setStartDateTime(newStart);
        booking.setEndDateTime(newEnd);
        booking.setStatus(BookingStatus.SCHEDULED);
        validateBooking(booking, id);
        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse markNoShow(UUID id) {
        var booking = getActiveBooking(id);
        booking.setStatus(BookingStatus.NO_SHOW);
        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse markCompleted(UUID id) {
        var booking = getActiveBooking(id);
        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }

    @Transactional
    public void delete(UUID id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        bookingRepository.delete(booking);
    }

    @Transactional(readOnly = true)
    public DashboardBookingMetricsResponse getDashboardMetrics(UUID studioId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
        LocalDateTime weekEndDateTime = todayEnd;

        long todayClasses = bookingRepository
                .countByStudioIdAndStartDateTimeBetweenAndActiveTrue(studioId, todayStart, todayEnd);
        long weekClasses = bookingRepository
                .countByStudioIdAndStartDateTimeBetweenAndActiveTrue(studioId, weekStartDateTime, weekEndDateTime);

        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
        LocalDateTime monthEndDateTime = monthEnd.atTime(LocalTime.MAX);

        long totalMonth = bookingRepository
                .countByStudioIdAndStartDateTimeBetween(studioId, monthStartDateTime, monthEndDateTime);
        long cancelledMonth = bookingRepository
                .countByStudioIdAndStatusAndStartDateTimeBetween(studioId, BookingStatus.CANCELLED,
                        monthStartDateTime, monthEndDateTime);
        long noShowMonth = bookingRepository
                .countByStudioIdAndStatusAndStartDateTimeBetween(studioId, BookingStatus.NO_SHOW,
                        monthStartDateTime, monthEndDateTime);

        double cancellationRate = totalMonth > 0 ? (double) cancelledMonth / totalMonth * 100 : 0;
        double noShowRate = totalMonth > 0 ? (double) noShowMonth / totalMonth * 100 : 0;
        double occupancyRate = totalMonth > 0 ? Math.min(100, (double) (totalMonth - cancelledMonth - noShowMonth) / totalMonth * 100) : 0;

        return new DashboardBookingMetricsResponse(todayClasses, weekClasses, occupancyRate, noShowRate, cancellationRate);
    }

    private Booking getActiveBooking(UUID id) {
        var booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!booking.getActive()) {
            throw new BusinessException("Booking is not active");
        }
        return booking;
    }

    private void validateBooking(Booking booking) {
        validateBooking(booking, null);
    }

    private void validateBooking(Booking booking, UUID excludeBookingId) {
        if (booking.getEndDateTime().isBefore(booking.getStartDateTime())
                || booking.getEndDateTime().isEqual(booking.getStartDateTime())) {
            throw new BusinessException("End time must be after start time");
        }

        if (booking.getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot create booking in the past");
        }

        var studentConflicts = bookingRepository
                .findConflictingByStudent(booking.getStudent().getId(),
                        booking.getStartDateTime(), booking.getEndDateTime())
                .stream().filter(b -> !b.getId().equals(excludeBookingId)).toList();
        if (!studentConflicts.isEmpty()) {
            throw new BusinessException("Student already has a booking in this time period");
        }

        var instructorConflicts = bookingRepository
                .findConflictingByInstructor(booking.getInstructor().getId(),
                        booking.getStartDateTime(), booking.getEndDateTime())
                .stream().filter(b -> !b.getId().equals(excludeBookingId)).toList();
        if (!instructorConflicts.isEmpty()) {
            throw new BusinessException("Instructor already has a booking in this time period");
        }

        if (booking.getRoomId() != null) {
            var roomConflicts = bookingRepository
                    .findConflictingByRoom(booking.getRoomId(),
                            booking.getStartDateTime(), booking.getEndDateTime())
                    .stream().filter(b -> !b.getId().equals(excludeBookingId)).toList();
            if (!roomConflicts.isEmpty()) {
                throw new BusinessException("Room already has a booking in this time period");
            }
        }

        boolean blocked = timeBlockService.isTimeBlocked(
                booking.getStudio().getId(),
                Long.valueOf(booking.getInstructor().getId().toString().hashCode() & 0x7fffffff),
                booking.getRoomId(),
                booking.getStartDateTime(),
                booking.getEndDateTime());

        if (blocked) {
            throw new BusinessException("Time period is blocked");
        }
    }

    private List<ConflictDTO> findConflicts(Booking booking) {
        List<ConflictDTO> conflicts = new ArrayList<>();

        bookingRepository.findConflictingByStudent(booking.getStudent().getId(),
                        booking.getStartDateTime(), booking.getEndDateTime())
                .forEach(b -> conflicts.add(new ConflictDTO("STUDENT",
                        "Student has another booking at this time", b.getId())));

        bookingRepository.findConflictingByInstructor(booking.getInstructor().getId(),
                        booking.getStartDateTime(), booking.getEndDateTime())
                .forEach(b -> conflicts.add(new ConflictDTO("INSTRUCTOR",
                        "Instructor has another booking at this time", b.getId())));

        if (booking.getRoomId() != null) {
            bookingRepository.findConflictingByRoom(booking.getRoomId(),
                            booking.getStartDateTime(), booking.getEndDateTime())
                    .forEach(b -> conflicts.add(new ConflictDTO("ROOM",
                            "Room has another booking at this time", b.getId())));
        }

        return conflicts;
    }

    private BookingResponse toResponse(Booking booking) {
        UUID originalId = booking.getOriginalBooking() != null ? booking.getOriginalBooking().getId() : null;
        return new BookingResponse(
                booking.getId(),
                booking.getStudio().getId(),
                booking.getStudent().getId(),
                booking.getStudent().getFullName(),
                booking.getInstructor().getId(),
                booking.getInstructor().getFullName(),
                booking.getRoomId(),
                booking.getClassType(),
                booking.getStartDateTime(),
                booking.getEndDateTime(),
                booking.getStatus(),
                booking.getCapacity(),
                booking.getMakeUpClass(),
                originalId,
                booking.getCancellationReason(),
                booking.getCancellationNotes(),
                booking.getActive(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

    private record ConflictDTO(String type, String description, UUID conflictingBookingId) {}
}
