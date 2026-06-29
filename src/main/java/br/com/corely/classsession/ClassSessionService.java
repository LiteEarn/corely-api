package br.com.corely.classsession;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.dto.ClassSessionRequest;
import br.com.corely.classsession.dto.ClassSessionResponse;
import br.com.corely.classsession.dto.SessionGenerationResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ConflictException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final ClassGroupRepository classGroupRepository;

    @Transactional
    public ClassSessionResponse create(ClassSessionRequest request) {
        ClassGroup classGroup = classGroupRepository.findById(request.getClassGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Turma inexistente"));

        if (!Boolean.TRUE.equals(classGroup.getActive())) {
            throw new ConflictException("Turma inativa");
        }

        if (!Boolean.TRUE.equals(classGroup.getInstructor().getActive())) {
            throw new ConflictException("Instrutor inativo");
        }

        if (request.getSessionDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Data da sessão não pode ser anterior à data atual");
        }

        if (classSessionRepository.existsByClassGroupIdAndSessionDate(
                request.getClassGroupId(), request.getSessionDate())) {
            throw new ConflictException("Já existe uma sessão para esta turma nesta data");
        }

        ClassSession classSession = new ClassSession();
        classSession.setClassGroup(classGroup);
        classSession.setInstructor(classGroup.getInstructor());
        classSession.setSessionDate(request.getSessionDate());
        classSession.setStartTime(classGroup.getStartTime());
        classSession.setEndTime(classGroup.getEndTime());
        classSession.setStatus(ClassSessionStatus.SCHEDULED);
        classSession.setNotes(request.getNotes());

        classSession = classSessionRepository.save(classSession);
        return toResponse(classSession);
    }

    @Transactional(readOnly = true)
    public ClassSessionResponse findById(UUID id) {
        ClassSession classSession = classSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão inexistente"));
        return toResponse(classSession);
    }

    @Transactional(readOnly = true)
    public List<ClassSessionResponse> findAll() {
        return classSessionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void cancel(UUID id) {
        ClassSession classSession = classSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão inexistente"));

        if (classSession.getStatus() == ClassSessionStatus.CANCELLED) {
            throw new ConflictException("Sessão já está cancelada");
        }

        classSession.setStatus(ClassSessionStatus.CANCELLED);
    }

    @Transactional
    public SessionGenerationResponse generateSessions(UUID classGroupId) {
        ClassGroup classGroup = classGroupRepository.findById(classGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Turma inexistente"));

        if (!Boolean.TRUE.equals(classGroup.getActive())) {
            throw new ConflictException("Não é possível gerar sessões para uma turma inativa.");
        }

        if (!Boolean.TRUE.equals(classGroup.getInstructor().getActive())) {
            throw new ConflictException("Instrutor inativo");
        }

        var dayOfWeekMap = java.util.Map.<DayOfWeek, Function<ClassGroup, Boolean>>of(
                DayOfWeek.MONDAY, ClassGroup::getMonday,
                DayOfWeek.TUESDAY, ClassGroup::getTuesday,
                DayOfWeek.WEDNESDAY, ClassGroup::getWednesday,
                DayOfWeek.THURSDAY, ClassGroup::getThursday,
                DayOfWeek.FRIDAY, ClassGroup::getFriday,
                DayOfWeek.SATURDAY, ClassGroup::getSaturday,
                DayOfWeek.SUNDAY, ClassGroup::getSunday
        );

        int created = 0;
        int ignored = 0;
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(60);

        for (LocalDate date = today; !date.isAfter(endDate); date = date.plusDays(1)) {
            Boolean active = dayOfWeekMap.get(date.getDayOfWeek()).apply(classGroup);
            if (!Boolean.TRUE.equals(active)) {
                continue;
            }

            if (classSessionRepository.existsByClassGroupIdAndSessionDate(classGroupId, date)) {
                ignored++;
                continue;
            }

            ClassSession session = new ClassSession();
            session.setClassGroup(classGroup);
            session.setInstructor(classGroup.getInstructor());
            session.setSessionDate(date);
            session.setStartTime(classGroup.getStartTime());
            session.setEndTime(classGroup.getEndTime());
            session.setStatus(ClassSessionStatus.SCHEDULED);
            classSessionRepository.save(session);
            created++;
        }

        return new SessionGenerationResponse(created, ignored);
    }

    private ClassSessionResponse toResponse(ClassSession classSession) {
        return new ClassSessionResponse(
                classSession.getId(),
                classSession.getClassGroup().getId(),
                classSession.getClassGroup().getName(),
                classSession.getInstructor().getId(),
                classSession.getInstructor().getFullName(),
                classSession.getSessionDate(),
                classSession.getStartTime(),
                classSession.getEndTime(),
                classSession.getStatus(),
                classSession.getNotes(),
                classSession.getClassGroup().getStudio().getId(),
                classSession.getCreatedAt(),
                classSession.getUpdatedAt()
        );
    }
}
