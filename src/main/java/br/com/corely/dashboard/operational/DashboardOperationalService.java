package br.com.corely.dashboard.operational;

import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.dashboard.operational.dto.*;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.makeup.MakeupRequest;
import br.com.corely.makeup.MakeupRequestRepository;
import br.com.corely.makeup.MakeupRequestStatus;
import br.com.corely.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardOperationalService {

    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClassGroupRepository classGroupRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MakeupRequestRepository makeupRequestRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public DashboardOperationalResponse getOperationalDashboard(UUID studioId) {
        LocalDate today = LocalDate.now();

        long classesToday = classSessionRepository.countByStudioIdAndSessionDate(studioId, today);
        long classesInProgress = classSessionRepository.countByStudioIdAndStatus(studioId, ClassSessionStatus.IN_PROGRESS);
        long activeStudents = studentRepository.countByStudioIdAndActiveTrue(studioId);
        long studentsPresentToday = attendanceRepository.countPresentByStudioIdAndSessionDate(studioId, today);
        long pendingMakeupCount = makeupRequestRepository.countByStudioIdAndStatus(studioId, MakeupRequestStatus.REQUESTED);

        SummaryResponse summary = new SummaryResponse(classesToday, classesInProgress, activeStudents, studentsPresentToday, pendingMakeupCount);

        List<UpcomingSessionResponse> upcomingSessions = buildUpcomingSessions(studioId, today);
        List<PendingMakeupResponse> pendingMakeupResponses = buildPendingMakeups(studioId);
        List<ClassOccupancyResponse> occupancyList = buildOccupancy(studioId);
        List<DashboardAlertResponse> alerts = buildAlerts(summary, occupancyList);

        return new DashboardOperationalResponse(summary, upcomingSessions, pendingMakeupResponses, occupancyList, alerts);
    }

    private List<UpcomingSessionResponse> buildUpcomingSessions(UUID studioId, LocalDate today) {
        List<ClassSession> sessions = classSessionRepository.findTodaySessionsByStudio(studioId, today);
        return sessions.stream()
                .map(s -> new UpcomingSessionResponse(
                        s.getId(),
                        s.getClassGroup().getId(),
                        s.getClassGroup().getName(),
                        s.getInstructor().getFullName(),
                        s.getStartTime(),
                        s.getEndTime(),
                        s.getStatus()
                ))
                .collect(Collectors.toList());
    }

    private List<PendingMakeupResponse> buildPendingMakeups(UUID studioId) {
        List<MakeupRequest> requests = makeupRequestRepository.findByStudioIdAndStatus(studioId, MakeupRequestStatus.REQUESTED);
        return requests.stream()
                .map(mr -> new PendingMakeupResponse(
                        mr.getId(),
                        mr.getAttendance().getEnrollment().getStudent().getFullName(),
                        mr.getAttendance().getEnrollment().getClassGroup().getName(),
                        mr.getRequestedAt(),
                        mr.getReason()
                ))
                .collect(Collectors.toList());
    }

    private List<ClassOccupancyResponse> buildOccupancy(UUID studioId) {
        List<ClassGroup> groups = classGroupRepository.findByStudioIdAndActiveTrue(studioId);
        return groups.stream()
                .map(cg -> {
                    long enrolled = enrollmentRepository.countByClassGroupIdAndActiveTrue(cg.getId());
                    int capacity = cg.getCapacity() != null ? cg.getCapacity() : 0;
                    int percent = capacity > 0 ? (int) Math.round((enrolled * 100.0) / capacity) : 0;
                    return new ClassOccupancyResponse(cg.getId(), cg.getName(), capacity, enrolled, percent);
                })
                .sorted(Comparator.comparingInt(ClassOccupancyResponse::getOccupancyPercent).reversed())
                .collect(Collectors.toList());
    }

    private List<DashboardAlertResponse> buildAlerts(SummaryResponse summary, List<ClassOccupancyResponse> occupancyList) {
        List<DashboardAlertResponse> alerts = new ArrayList<>();

        for (ClassOccupancyResponse occ : occupancyList) {
            if (occ.getOccupancyPercent() >= 90) {
                alerts.add(new DashboardAlertResponse("Turma quase lotada: " + occ.getClassName()));
            }
        }

        if (summary.getPendingMakeupRequests() > 10) {
            alerts.add(new DashboardAlertResponse("Muitas reposições pendentes"));
        }

        if (summary.getClassesToday() == 0) {
            alerts.add(new DashboardAlertResponse("Nenhuma aula programada"));
        }

        return alerts;
    }
}
