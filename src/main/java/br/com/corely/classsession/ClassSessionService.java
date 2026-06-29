package br.com.corely.classsession;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classsession.dto.ClassSessionRequest;
import br.com.corely.classsession.dto.ClassSessionResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ConflictException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
