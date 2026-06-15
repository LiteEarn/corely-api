package br.com.corely.classsession;

import br.com.corely.classsession.dto.ClassSessionRequest;
import br.com.corely.classsession.dto.ClassSessionResponse;
import br.com.corely.instructor.Instructor;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final StudioRepository studioRepository;
    private final InstructorRepository instructorRepository;

    @Transactional
    public ClassSessionResponse create(ClassSessionRequest request) {
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Instructor instructor = instructorRepository.findById(request.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));

        ClassSession classSession = new ClassSession();
        classSession.setStudio(studio);
        classSession.setInstructor(instructor);
        classSession.setTitle(request.getTitle());
        classSession.setScheduledDate(request.getScheduledDate());
        classSession.setStartTime(request.getStartTime());
        classSession.setEndTime(request.getEndTime());
        classSession.setMaxStudents(request.getMaxStudents());
        classSession.setStatus(request.getStatus() != null ? request.getStatus() : ClassSessionStatus.SCHEDULED);

        classSession = classSessionRepository.save(classSession);
        return toResponse(classSession);
    }

    @Transactional(readOnly = true)
    public List<ClassSessionResponse> findAll() {
        return classSessionRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClassSessionResponse findById(UUID id) {
        ClassSession classSession = classSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class session not found"));
        return toResponse(classSession);
    }

    @Transactional
    public ClassSessionResponse update(UUID id, ClassSessionRequest request) {
        ClassSession classSession = classSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class session not found"));

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Instructor instructor = instructorRepository.findById(request.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));

        classSession.setStudio(studio);
        classSession.setInstructor(instructor);
        classSession.setTitle(request.getTitle());
        classSession.setScheduledDate(request.getScheduledDate());
        classSession.setStartTime(request.getStartTime());
        classSession.setEndTime(request.getEndTime());
        classSession.setMaxStudents(request.getMaxStudents());
        if (request.getStatus() != null) {
            classSession.setStatus(request.getStatus());
        }

        classSession = classSessionRepository.save(classSession);
        return toResponse(classSession);
    }

    @Transactional
    public void delete(UUID id) {
        ClassSession classSession = classSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class session not found"));
        classSessionRepository.delete(classSession);
    }

    private ClassSessionResponse toResponse(ClassSession classSession) {
        return new ClassSessionResponse(
                classSession.getId(),
                classSession.getTitle(),
                classSession.getScheduledDate(),
                classSession.getStartTime(),
                classSession.getEndTime(),
                classSession.getMaxStudents(),
                classSession.getStatus(),
                classSession.getInstructor().getId(),
                classSession.getInstructor().getFullName(),
                classSession.getStudio().getId(),
                classSession.getCreatedAt(),
                classSession.getUpdatedAt()
        );
    }
}
