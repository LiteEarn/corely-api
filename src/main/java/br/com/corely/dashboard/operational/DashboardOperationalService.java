package br.com.corely.dashboard.operational;

import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.attendance.AttendanceStatus;
import br.com.corely.attendance.dto.AttendanceResponse;
import br.com.corely.attendance.AttendanceService;
import br.com.corely.classgroup.ClassGroupService;
import br.com.corely.classgroup.dto.ClassGroupResponse;
import br.com.corely.classsession.ClassSessionService;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.classsession.dto.ClassSessionResponse;
import br.com.corely.dashboard.operational.dto.*;
import br.com.corely.enrollment.EnrollmentService;
import br.com.corely.makeup.MakeupRequest;
import br.com.corely.makeup.MakeupRequestRepository;
import br.com.corely.makeup.MakeupRequestService;
import br.com.corely.makeup.MakeupRequestStatus;
import br.com.corely.makeup.dto.MakeupRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardOperationalService {

    private final ClassSessionService classSessionService;
    private final AttendanceService attendanceService;
    private final AttendanceRepository attendanceRepository;
    private final ClassGroupService classGroupService;
    private final EnrollmentService enrollmentService;
    private final MakeupRequestService makeupRequestService;
    private final MakeupRequestRepository makeupRequestRepository;

    @Transactional(readOnly = true)
    public OperationalDashboardResponse getOperationalDashboard(UUID studioId) {
        LocalDate today = LocalDate.now();

        List<ClassSessionResponse> todaySessions = filterByStudio(
                classSessionService.findAll(null, null, null, today), studioId);
        todaySessions.sort(Comparator.comparing(ClassSessionResponse::getStartTime));

        List<ClassSessionResponse> inProgressSessions = filterByStudio(
                classSessionService.findAll(null, null, ClassSessionStatus.IN_PROGRESS, null), studioId);

        List<MakeupRequest> studioPendingMakeups = findPendingMakeupsByStudio(studioId);

        SummaryResponse summary = buildSummary(todaySessions, inProgressSessions, studioPendingMakeups);
        List<UpcomingSessionResponse> upcomingSessions = buildUpcomingSessions(todaySessions);
        List<PendingMakeupResponse> pendingMakeupResponses = buildPendingMakeups(studioPendingMakeups);
        List<OccupancyResponse> occupancyList = buildOccupancy(studioId);
        List<DashboardAlertResponse> alerts = buildAlerts(summary, occupancyList);

        return new OperationalDashboardResponse(summary, upcomingSessions, pendingMakeupResponses, occupancyList, alerts);
    }

    private SummaryResponse buildSummary(List<ClassSessionResponse> todaySessions,
                                          List<ClassSessionResponse> inProgressSessions,
                                          List<MakeupRequest> studioPendingMakeups) {
        long classesToday = todaySessions.size();
        long classesInProgress = inProgressSessions.size();

        long studentsPresentToday = 0;
        for (ClassSessionResponse session : todaySessions) {
            List<AttendanceResponse> attendances = attendanceService.findBySessionId(session.getId());
            studentsPresentToday += attendances.stream()
                    .filter(a -> a.status() == AttendanceStatus.PRESENT)
                    .count();
        }

        long pendingMakeupCount = studioPendingMakeups.size();

        return new SummaryResponse(classesToday, classesInProgress, studentsPresentToday, pendingMakeupCount);
    }

    private List<UpcomingSessionResponse> buildUpcomingSessions(List<ClassSessionResponse> todaySessions) {
        return todaySessions.stream()
                .map(session -> {
                    long enrolled = enrollmentService.findStudentsByClassGroupId(session.getClassGroupId()).size();
                    return new UpcomingSessionResponse(
                            session.getId(),
                            session.getClassGroupId(),
                            session.getClassGroupName(),
                            session.getSessionDate(),
                            session.getStartTime(),
                            session.getEndTime(),
                            session.getStatus(),
                            session.getInstructorName(),
                            enrolled
                    );
                })
                .collect(Collectors.toList());
    }

    private List<PendingMakeupResponse> buildPendingMakeups(List<MakeupRequest> studioPendingMakeups) {
        return studioPendingMakeups.stream()
                .map(mr -> {
                    String studentName = mr.getAttendance().getEnrollment().getStudent().getFullName();
                    return new PendingMakeupResponse(
                            mr.getId(),
                            studentName,
                            mr.getRequestedAt(),
                            mr.getReason()
                    );
                })
                .collect(Collectors.toList());
    }

    private List<OccupancyResponse> buildOccupancy(UUID studioId) {
        List<ClassGroupResponse> activeGroups = classGroupService.findActive();
        return activeGroups.stream()
                .filter(cg -> cg.getStudioId().equals(studioId))
                .map(cg -> {
                    long enrolled = enrollmentService.findStudentsByClassGroupId(cg.getId()).size();
                    int capacity = cg.getCapacity() != null ? cg.getCapacity() : 0;
                    int percentage = capacity > 0 ? (int) Math.round((enrolled * 100.0) / capacity) : 0;
                    return new OccupancyResponse(cg.getId(), cg.getName(), capacity, enrolled, percentage);
                })
                .collect(Collectors.toList());
    }

    private List<DashboardAlertResponse> buildAlerts(SummaryResponse summary, List<OccupancyResponse> occupancyList) {
        List<String> messages = new ArrayList<>();

        for (OccupancyResponse occ : occupancyList) {
            if (occ.getOccupancyPercentage() > 90) {
                messages.add("Turma lotada: " + occ.getClassGroupName());
            }
        }

        if (summary.getPendingMakeupRequests() > 0) {
            messages.add("Reposições pendentes: " + summary.getPendingMakeupRequests());
        }

        if (summary.getClassesInProgress() > 0) {
            messages.add("Aulas em andamento: " + summary.getClassesInProgress());
        }

        if (summary.getClassesToday() == 0) {
            messages.add("Nenhuma aula hoje");
        }

        return messages.stream()
                .map(DashboardAlertResponse::new)
                .collect(Collectors.toList());
    }

    private List<MakeupRequest> findPendingMakeupsByStudio(UUID studioId) {
        return makeupRequestRepository.findByStatus(MakeupRequestStatus.REQUESTED)
                .stream()
                .filter(mr -> mr.getAttendance().getEnrollment().getStudio().getId().equals(studioId))
                .collect(Collectors.toList());
    }

    private List<ClassSessionResponse> filterByStudio(List<ClassSessionResponse> sessions, UUID studioId) {
        return sessions.stream()
                .filter(s -> s.getStudioId().equals(studioId))
                .collect(Collectors.toList());
    }
}
