package br.com.corely.dashboard.operational;

import br.com.corely.attendance.AttendanceRepository;
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

        DashboardKpiResponse kpis = buildKpis(studioId, today);
        List<UpcomingSessionResponse> upcomingSessions = buildUpcomingSessions(studioId, today);
        List<PendingMakeupResponse> pendingMakeupRequests = buildPendingMakeups(studioId);
        List<ClassOccupancyResponse> classOccupancy = buildOccupancy(studioId);
        Integer averageOccupancy = calculateAverageOccupancy(classOccupancy);
        Integer todayAttendanceRate = calculateTodayAttendanceRate(studioId, today, kpis.getStudentsPresentToday());
        List<DashboardAlertResponse> alerts = buildAlerts(kpis, classOccupancy);

        SummaryResponse summary = new SummaryResponse(kpis, averageOccupancy, todayAttendanceRate);
        return new DashboardOperationalResponse(summary, upcomingSessions, pendingMakeupRequests, classOccupancy, alerts);
    }

    private DashboardKpiResponse buildKpis(UUID studioId, LocalDate today) {
        long classesToday = classSessionRepository.countByStudioIdAndSessionDate(studioId, today);
        long classesInProgress = classSessionRepository.countByStudioIdAndStatus(studioId, ClassSessionStatus.IN_PROGRESS);
        long activeStudents = studentRepository.countByStudioIdAndActiveTrue(studioId);
        long studentsPresentToday = attendanceRepository.countPresentByStudioIdAndSessionDate(studioId, today);
        long pendingMakeupCount = makeupRequestRepository.countByStudioIdAndStatus(studioId, MakeupRequestStatus.REQUESTED);

        return new DashboardKpiResponse(classesToday, classesInProgress, activeStudents, studentsPresentToday, pendingMakeupCount);
    }

    private List<UpcomingSessionResponse> buildUpcomingSessions(UUID studioId, LocalDate today) {
        List<ClassSession> sessions = classSessionRepository.findTodaySessionsByStudio(studioId, today);
        return sessions.stream()
                .map(this::toUpcomingSessionResponse)
                .sorted(Comparator.comparing((UpcomingSessionResponse s) ->
                                s.getStatus() != ClassSessionStatus.IN_PROGRESS)
                        .thenComparing(UpcomingSessionResponse::getStartTime))
                .limit(5)
                .toList();
    }

    private UpcomingSessionResponse toUpcomingSessionResponse(ClassSession s) {
        long enrolledStudents = enrollmentRepository.countByClassGroupIdAndActiveTrue(s.getClassGroup().getId());
        return new UpcomingSessionResponse(
                s.getId(),
                s.getClassGroup().getId(),
                s.getClassGroup().getName(),
                s.getInstructor().getId(),
                s.getInstructor().getFullName(),
                s.getStartTime(),
                s.getEndTime(),
                enrolledStudents,
                s.getStatus()
        );
    }

    private List<PendingMakeupResponse> buildPendingMakeups(UUID studioId) {
        List<MakeupRequest> requests = makeupRequestRepository.findByStudioIdAndStatus(studioId, MakeupRequestStatus.REQUESTED);
        return requests.stream()
                .sorted(Comparator.comparing(MakeupRequest::getRequestedAt))
                .limit(5)
                .map(this::toPendingMakeupResponse)
                .toList();
    }

    private PendingMakeupResponse toPendingMakeupResponse(MakeupRequest mr) {
        return new PendingMakeupResponse(
                mr.getId(),
                mr.getAttendance().getEnrollment().getClassGroup().getId(),
                mr.getAttendance().getEnrollment().getStudent().getFullName(),
                mr.getAttendance().getEnrollment().getClassGroup().getName(),
                mr.getAttendance().getClassSession().getSessionDate(),
                mr.getReason()
        );
    }

    private List<ClassOccupancyResponse> buildOccupancy(UUID studioId) {
        List<Object[]> results = classGroupRepository.findActiveClassGroupsWithEnrollmentCount(studioId);
        return results.stream()
                .map(this::toClassOccupancyResponse)
                .sorted(Comparator.comparingInt(ClassOccupancyResponse::getOccupancyPercent).reversed())
                .limit(5)
                .toList();
    }

    private ClassOccupancyResponse toClassOccupancyResponse(Object[] row) {
        UUID classGroupId = (UUID) row[0];
        String className = (String) row[1];
        int capacity = row[2] != null ? ((Number) row[2]).intValue() : 0;
        long enrolled = row[3] != null ? ((Number) row[3]).longValue() : 0;
        int percent = capacity > 0 ? (int) Math.round((enrolled * 100.0) / capacity) : 0;
        return new ClassOccupancyResponse(classGroupId, className, capacity, enrolled, percent);
    }

    private Integer calculateAverageOccupancy(List<ClassOccupancyResponse> occupancyList) {
        if (occupancyList.isEmpty()) {
            return 0;
        }
        int sum = occupancyList.stream()
                .mapToInt(ClassOccupancyResponse::getOccupancyPercent)
                .sum();
        return (int) Math.round((double) sum / occupancyList.size());
    }

    private Integer calculateTodayAttendanceRate(UUID studioId, LocalDate today, long presentStudents) {
        long totalEnrolledToday = enrollmentRepository.countEnrolledTodayByStudioId(studioId, today);
        if (totalEnrolledToday == 0) {
            return 0;
        }
        return (int) Math.round((presentStudents * 100.0) / totalEnrolledToday);
    }

    private List<DashboardAlertResponse> buildAlerts(DashboardKpiResponse kpis, List<ClassOccupancyResponse> occupancyList) {
        List<DashboardAlertResponse> alerts = new ArrayList<>();
        Set<AlertType> addedTypes = new HashSet<>();

        checkFullClass(alerts, addedTypes, occupancyList);
        checkPendingMakeup(alerts, addedTypes, kpis.getPendingMakeups());
        checkNoClasses(alerts, addedTypes, kpis.getClassesToday());
        checkLowOccupancy(alerts, addedTypes, occupancyList);

        return alerts;
    }

    private void checkFullClass(List<DashboardAlertResponse> alerts, Set<AlertType> addedTypes,
                                 List<ClassOccupancyResponse> occupancyList) {
        if (alerts.size() >= 5 || addedTypes.contains(AlertType.FULL_CLASS)) {
            return;
        }
        occupancyList.stream()
                .filter(o -> o.getOccupancyPercent() >= 90)
                .findFirst()
                .ifPresent(occ -> {
                    alerts.add(new DashboardAlertResponse(
                            "Turma Lotada",
                            "Turma '" + occ.getClassName() + "' está com " + occ.getOccupancyPercent() + "% de ocupação",
                            AlertSeverity.ERROR,
                            AlertType.FULL_CLASS,
                            "Ver turma",
                            "/class-groups",
                            occ.getClassGroupId()
                    ));
                    addedTypes.add(AlertType.FULL_CLASS);
                });
    }

    private void checkPendingMakeup(List<DashboardAlertResponse> alerts, Set<AlertType> addedTypes, long pendingCount) {
        if (alerts.size() >= 5 || addedTypes.contains(AlertType.PENDING_MAKEUP)) {
            return;
        }
        if (pendingCount > 10) {
            alerts.add(new DashboardAlertResponse(
                    "Muitas Reposições",
                    pendingCount + " reposições pendentes aguardando aprovação",
                    AlertSeverity.WARNING,
                    AlertType.PENDING_MAKEUP,
                    "Ver reposições",
                    "/makeup-requests",
                    null
            ));
            addedTypes.add(AlertType.PENDING_MAKEUP);
        }
    }

    private void checkNoClasses(List<DashboardAlertResponse> alerts, Set<AlertType> addedTypes, long classesToday) {
        if (alerts.size() >= 5 || addedTypes.contains(AlertType.NO_CLASSES)) {
            return;
        }
        if (classesToday == 0) {
            alerts.add(new DashboardAlertResponse(
                    "Nenhuma Aula",
                    "Nenhuma aula programada para hoje",
                    AlertSeverity.WARNING,
                    AlertType.NO_CLASSES,
                    "Agendar aula",
                    "/class-sessions",
                    null
            ));
            addedTypes.add(AlertType.NO_CLASSES);
        }
    }

    private void checkLowOccupancy(List<DashboardAlertResponse> alerts, Set<AlertType> addedTypes,
                                    List<ClassOccupancyResponse> occupancyList) {
        if (alerts.size() >= 5 || addedTypes.contains(AlertType.LOW_OCCUPANCY)) {
            return;
        }
        occupancyList.stream()
                .filter(o -> o.getOccupancyPercent() < 30)
                .findFirst()
                .ifPresent(occ -> {
                    alerts.add(new DashboardAlertResponse(
                            "Baixa Ocupação",
                            "Turma '" + occ.getClassName() + "' está com apenas " + occ.getOccupancyPercent() + "% de ocupação",
                            AlertSeverity.INFO,
                            AlertType.LOW_OCCUPANCY,
                            "Ver turma",
                            "/class-groups",
                            occ.getClassGroupId()
                    ));
                    addedTypes.add(AlertType.LOW_OCCUPANCY);
                });
    }
}
