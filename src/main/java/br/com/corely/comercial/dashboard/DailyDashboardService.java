package br.com.corely.comercial.dashboard;

import br.com.corely.comercial.attendance.AttendanceRepository;
import br.com.corely.comercial.booking.BookingRepository;
import br.com.corely.comercial.classsession.ClassSession;
import br.com.corely.comercial.classsession.ClassSessionRepository;
import br.com.corely.comercial.classsession.SessionStatus;
import br.com.corely.comercial.dashboard.dto.DailyDashboardResponse;
import br.com.corely.comercial.dashboard.dto.SessionDashboardResponse;
import br.com.corely.comercial.makeup.MakeUpRepository;
import br.com.corely.comercial.waitlist.WaitListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyDashboardService {

    private final ClassSessionRepository classSessionRepository;
    private final BookingRepository bookingRepository;
    private final AttendanceRepository attendanceRepository;
    private final WaitListRepository waitListRepository;
    private final MakeUpRepository makeUpRepository;

    @Transactional(readOnly = true)
    public DailyDashboardResponse getDailyDashboard(LocalDate date) {
        var sessions = classSessionRepository.findBySessionDateWithSlotAndSchedule(date);

        var sessionIds = sessions.stream()
                .map(s -> s.getId())
                .toList();

        var confirmedBySession = buildMap(bookingRepository.countConfirmedBySessionIds(sessionIds));
        var presentBySession = buildMap(attendanceRepository.countPresentBySessionIds(sessionIds));
        var absentBySession = buildMap(attendanceRepository.countAbsentBySessionIds(sessionIds));
        var waitingBySession = buildMap(waitListRepository.countWaitingBySessionIds(sessionIds));
        var makeupBySession = buildMap(makeUpRepository.countByOriginalSessionIds(sessionIds));

        long totalConfirmed = bookingRepository.countConfirmedBySessionIdList(sessionIds);
        long totalPresent = attendanceRepository.countPresentBySessionIdList(sessionIds);
        long totalAbsent = attendanceRepository.countAbsentBySessionIdList(sessionIds);

        long totalSessions = sessions.size();
        long started = sessions.stream().filter(s -> s.getStatus() == br.com.corely.comercial.classsession.SessionStatus.IN_PROGRESS).count();
        long finished = sessions.stream().filter(s -> s.getStatus() == br.com.corely.comercial.classsession.SessionStatus.FINISHED).count();
        long cancelled = sessions.stream().filter(s -> s.getStatus() == br.com.corely.comercial.classsession.SessionStatus.CANCELLED).count();

        int totalCapacity = sessions.stream().filter(s -> s.getStatus() != br.com.corely.comercial.classsession.SessionStatus.CANCELLED).mapToInt(ClassSession::getCapacity).sum();
        int totalBooked = sessions.stream().filter(s -> s.getStatus() != br.com.corely.comercial.classsession.SessionStatus.CANCELLED).mapToInt(ClassSession::getBookedCount).sum();
        int freeSlots = totalCapacity - totalBooked;

        var sessionResponses = sessions.stream()
                .map(s -> buildSessionResponse(s, confirmedBySession, waitingBySession, makeupBySession))
                .toList();

        return new DailyDashboardResponse(
                date,
                (long) sessions.size(),
                started,
                finished,
                cancelled,
                totalCapacity,
                totalBooked,
                freeSlots,
                totalConfirmed,
                totalPresent,
                totalAbsent,
                sessionResponses
        );
    }

    private SessionDashboardResponse buildSessionResponse(
            br.com.corely.comercial.classsession.ClassSession session,
            java.util.Map<UUID, Long> confirmedBySession,
            java.util.Map<UUID, Long> waitingBySession,
            java.util.Map<UUID, Long> makeupBySession) {

        var slot = session.getScheduleSlot();
        var schedule = slot.getSchedule();

        int vagasOcupadas = session.getBookedCount();
        int vagasDisponiveis = session.getCapacity() - vagasOcupadas;
        long presencas = confirmedBySession.getOrDefault(session.getId(), 0L);
        long faltas = 0L;
        long espera = waitingBySession.getOrDefault(session.getId(), 0L);
        long creditos = makeupBySession.getOrDefault(session.getId(), 0L);

        return new SessionDashboardResponse(
                session.getId(),
                session.getStartTime(),
                schedule.getName(),
                null,
                session.getStatus(),
                session.getCapacity(),
                vagasOcupadas,
                vagasDisponiveis,
                presencas,
                faltas,
                espera,
                creditos
        );
    }

    private java.util.Map<UUID, Long> buildMap(List<Object[]> rows) {
        var map = new java.util.HashMap<UUID, Long>();
        if (rows != null) {
            for (Object[] row : rows) {
                map.put((UUID) row[0], (Long) row[1]);
            }
        }
        return map;
    }
}
