package br.com.corely.dashboard;

import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.dashboard.dto.DashboardResponse;
import br.com.corely.dashboard.dto.RecentEvaluationDTO;
import br.com.corely.dashboard.dto.RecentEvolutionDTO;
import br.com.corely.dashboard.operational.DashboardOperationalService;
import br.com.corely.dashboard.operational.dto.DashboardOperationalResponse;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.evaluation.Evaluation;
import br.com.corely.evaluation.EvaluationRepository;
import br.com.corely.evolution.Evolution;
import br.com.corely.evolution.EvolutionRepository;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.objective.ObjectiveRepository;
import br.com.corely.objective.ObjectiveStatus;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardOperationalService dashboardOperationalService;

    private final StudioRepository studioRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;
    private final ClassGroupRepository classGroupRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ObjectiveRepository objectiveRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvolutionRepository evolutionRepository;

    @Transactional(readOnly = true)
    public DashboardOperationalResponse getOperationalDashboard(UUID studioId) {
        UUID resolvedStudioId = resolveStudioId(studioId);
        return dashboardOperationalService.getOperationalDashboard(resolvedStudioId);
    }

    private UUID resolveStudioId(UUID studioId) {
        if (studioId != null) {
            return studioId;
        }
        return studioRepository.findFirstByActiveTrueOrderByName()
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum studio ativo encontrado"))
                .getId();
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID studioId) {
        Studio studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        long activeStudents = studentRepository.countByStudioIdAndActiveTrue(studioId);
        long activeInstructors = instructorRepository.countByStudioIdAndActiveTrue(studioId);
        long activeClassGroups = classGroupRepository.countByStudioIdAndActiveTrue(studioId);
        long totalEnrollments = enrollmentRepository.countByStudioIdAndActiveTrue(studioId);
        long attendanceThisWeek = attendanceRepository.countByStudioIdAndSessionDateBetweenAndPresent(
                studioId, weekAgo, today
        );
        long attendanceThisMonth = attendanceRepository.countByStudioIdAndSessionDateBetweenAndPresent(
                studioId, monthStart, monthEnd
        );
        long activeObjectives = objectiveRepository.countByStudioIdAndStatus(studioId, ObjectiveStatus.ACTIVE);
        long completedObjectives = objectiveRepository.countByStudioIdAndStatus(studioId, ObjectiveStatus.COMPLETED);
        long evaluationsThisMonth = evaluationRepository.countByStudioIdAndEvaluationDateBetween(
                studioId, monthStart, monthEnd
        );
        long evolutionsThisMonth = evolutionRepository.countByStudioIdAndEvolutionDateBetween(
                studioId, monthStart, monthEnd
        );

        long totalCapacity = classGroupRepository.sumCapacityByStudioIdAndActiveTrue(studioId);
        BigDecimal occupancyRate = calculateOccupancyRate(totalEnrollments, totalCapacity);

        List<RecentEvaluationDTO> recentEvaluations = getRecentEvaluations(studioId);
        List<RecentEvolutionDTO> recentEvolutions = getRecentEvolutions(studioId);

        return DashboardResponse.builder()
                .activeStudents(activeStudents)
                .activeInstructors(activeInstructors)
                .activeClassGroups(activeClassGroups)
                .totalEnrollments(totalEnrollments)
                .attendanceThisWeek(attendanceThisWeek)
                .attendanceThisMonth(attendanceThisMonth)
                .activeObjectives(activeObjectives)
                .completedObjectives(completedObjectives)
                .evaluationsThisMonth(evaluationsThisMonth)
                .evolutionsThisMonth(evolutionsThisMonth)
                .occupancyRate(occupancyRate)
                .recentEvaluations(recentEvaluations)
                .recentEvolutions(recentEvolutions)
                .build();
    }

    private BigDecimal calculateOccupancyRate(long totalEnrollments, long totalCapacity) {
        if (totalCapacity == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = BigDecimal.valueOf(totalEnrollments)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCapacity), 2, RoundingMode.HALF_UP);
        return rate;
    }

    private List<RecentEvaluationDTO> getRecentEvaluations(UUID studioId) {
        List<Evaluation> evaluations = evaluationRepository.findByStudioIdOrderByCreatedAtDesc(studioId);
        return evaluations.stream()
                .limit(5)
                .map(this::toRecentEvaluationDTO)
                .collect(Collectors.toList());
    }

    private List<RecentEvolutionDTO> getRecentEvolutions(UUID studioId) {
        List<Evolution> evolutions = evolutionRepository.findByStudioIdOrderByCreatedAtDesc(studioId);
        return evolutions.stream()
                .limit(5)
                .map(this::toRecentEvolutionDTO)
                .collect(Collectors.toList());
    }

    private RecentEvaluationDTO toRecentEvaluationDTO(Evaluation evaluation) {
        return RecentEvaluationDTO.builder()
                .id(evaluation.getId())
                .studentId(evaluation.getStudent().getId())
                .studentName(evaluation.getStudent().getFullName())
                .evaluationDate(evaluation.getEvaluationDate())
                .weight(evaluation.getWeight())
                .height(evaluation.getHeight())
                .observations(evaluation.getObservations())
                .createdAt(evaluation.getCreatedAt())
                .build();
    }

    private RecentEvolutionDTO toRecentEvolutionDTO(Evolution evolution) {
        return RecentEvolutionDTO.builder()
                .id(evolution.getId())
                .studentId(evolution.getStudent().getId())
                .studentName(evolution.getStudent().getFullName())
                .evolutionDate(evolution.getEvolutionDate())
                .title(evolution.getTitle())
                .description(evolution.getDescription())
                .createdBy(evolution.getCreatedBy())
                .createdAt(evolution.getCreatedAt())
                .build();
    }
}
