package br.com.corely.enrollment;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.enrollment.dto.EnrollmentRequest;
import br.com.corely.enrollment.dto.EnrollmentResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudioRepository studioRepository;
    private final StudentRepository studentRepository;
    private final ClassGroupRepository classGroupRepository;

    @Transactional
    public EnrollmentResponse create(EnrollmentRequest request) {
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        validateStudentActiveForEnrollment(student);
        validateStudantClassGroupUnique(request, student);

        ClassGroup classGroup = classGroupRepository.findById(request.getClassGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));

        validateClassGroupActiveForEnrollment(classGroup);
        validateClassGroupCapacity(classGroup.getCapacity(), studio.getId(), request.getClassGroupId());

        enrollmentRepository.findByStudentIdAndClassGroupId(request.getStudentId(), request.getClassGroupId())
                .ifPresent(enrollment -> {
                    throw new IllegalArgumentException("Student is already enrolled in this class group");
                });

        Enrollment enrollment = new Enrollment();
        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(request.getEnrollmentDate() != null ? request.getEnrollmentDate() : LocalDate.now());
        enrollment.setActive(request.getActive() != null ? request.getActive() : true);

        enrollment = enrollmentRepository.save(enrollment);
        return toResponse(enrollment);
    }

    private void validateStudantClassGroupUnique(EnrollmentRequest request, Student student) {
        enrollmentRepository.findByStudentIdAndActiveTrue(student.getId()).stream().findFirst()
                .ifPresent(existingEnrollment -> {
                    if (existingEnrollment.getClassGroup().getId().equals(request.getClassGroupId())) {
                        throw new BusinessException("Estudante já está matriculado neste grupo de aula");
                    }
                });
    }

    private void validateClassGroupCapacity(Integer capacity, UUID studioId, UUID classGroupId) {

        if (capacity != null) {
            long enrolledCount = enrollmentRepository.countByStudioIdAndClassGroup_IdAndActiveTrue(studioId,classGroupId);

            if (enrolledCount >= capacity) {
                throw new BusinessException("Classe cheia");
            }
        }


    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findAll() {
        return enrollmentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse findById(UUID id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        return toResponse(enrollment);
    }

    @Transactional
    public EnrollmentResponse update(UUID id, EnrollmentRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!student.getId().equals(enrollment.getStudent().getId())) {
            validateStudentActiveForEnrollment(student);
        }

        ClassGroup classGroup = classGroupRepository.findById(request.getClassGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));

        validateClassGroupActiveForEnrollment(classGroup);

        enrollmentRepository.findByStudentIdAndClassGroupId(request.getStudentId(), request.getClassGroupId())
                .ifPresent(existingEnrollment -> {
                    if (!existingEnrollment.getId().equals(id)) {
                        throw new IllegalArgumentException("Student is already enrolled in this class group");
                    }
                });

        enrollment.setStudio(studio);
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        if (request.getEnrollmentDate() != null) {
            enrollment.setEnrollmentDate(request.getEnrollmentDate());
        }
        if (request.getActive() != null) {
            enrollment.setActive(request.getActive());
        }

        enrollment = enrollmentRepository.save(enrollment);
        return toResponse(enrollment);
    }

    @Transactional
    public void delete(UUID id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        enrollmentRepository.delete(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findStudentsByClassGroupId(UUID classGroupId) {
        return enrollmentRepository.findByClassGroupIdAndActiveTrueAndStudentActiveTrueAndClassGroupActiveTrue(classGroupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void validateStudentActiveForEnrollment(Student student) {
        if (!Boolean.TRUE.equals(student.getActive())) {
            throw new BusinessException("Não é possível matricular um aluno inativo.");
        }
    }

    private void validateClassGroupActiveForEnrollment(ClassGroup classGroup) {
        if (!Boolean.TRUE.equals(classGroup.getActive())) {
            throw new BusinessException("The selected class group is inactive.");
        }
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudio().getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getFullName(),
                enrollment.getClassGroup().getId(),
                enrollment.getClassGroup().getName(),
                enrollment.getEnrollmentDate(),
                enrollment.getActive(),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }
}
