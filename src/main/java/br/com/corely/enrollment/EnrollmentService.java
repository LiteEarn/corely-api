package br.com.corely.enrollment;

import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.enrollment.dto.EnrollmentRequest;
import br.com.corely.enrollment.dto.EnrollmentResponse;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ConflictException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
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
    private final StudentRepository studentRepository;
    private final ClassGroupRepository classGroupRepository;

    @Transactional
    public EnrollmentResponse create(EnrollmentRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno inexistente"));

        ClassGroup classGroup = classGroupRepository.findById(request.getClassGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Turma inexistente"));

        validateStudioIdMatch(student, classGroup, request.getStudioId());
        validateStudentActiveForEnrollment(student);
        validateClassGroupActiveForEnrollment(classGroup);
        validateInstructorActiveForEnrollment(classGroup);
        validateEnrollmentDate(request.getEnrollmentDate());
        validateClassGroupCapacity(classGroup.getCapacity(), request.getStudioId(), request.getClassGroupId());
        validateUniqueEnrollment(request.getStudentId(), request.getClassGroupId());

        Enrollment enrollment = new Enrollment();
        enrollment.setStudio(student.getStudio());
        enrollment.setStudent(student);
        enrollment.setClassGroup(classGroup);
        enrollment.setEnrollmentDate(request.getEnrollmentDate() != null ? request.getEnrollmentDate() : LocalDate.now());
        enrollment.setActive(request.getActive() != null ? request.getActive() : true);

        enrollment = enrollmentRepository.save(enrollment);
        return toResponse(enrollment);
    }

    private void validateStudioIdMatch(Student student, ClassGroup classGroup, UUID studioId) {
        if (!student.getStudio().getId().equals(studioId)) {
            throw new BusinessException("Aluno não pertence ao studio informado");
        }
        if (!classGroup.getStudio().getId().equals(studioId)) {
            throw new BusinessException("Turma não pertence ao studio informado");
        }
    }

    private void validateInstructorActiveForEnrollment(ClassGroup classGroup) {
        if (!Boolean.TRUE.equals(classGroup.getInstructor().getActive())) {
            throw new ConflictException("Instrutor inativo");
        }
    }

    private void validateEnrollmentDate(LocalDate enrollmentDate) {
        LocalDate dateToValidate = enrollmentDate != null ? enrollmentDate : LocalDate.now();
        if (dateToValidate.isAfter(LocalDate.now())) {
            throw new BusinessException("Data de matrícula não pode ser futura");
        }
    }

    private void validateUniqueEnrollment(UUID studentId, UUID classGroupId) {
        enrollmentRepository.findByStudentIdAndClassGroupId(studentId, classGroupId)
                .ifPresent(enrollment -> {
                    throw new ConflictException("Aluno já matriculado");
                });
    }

    private void validateClassGroupCapacity(Integer capacity, UUID studioId, UUID classGroupId) {
        if (capacity != null) {
            long enrolledCount = enrollmentRepository.countByStudioIdAndClassGroup_IdAndActiveTrue(studioId, classGroupId);

            if (enrolledCount >= capacity) {
                throw new ConflictException("Turma cheia");
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
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula inexistente"));
        return toResponse(enrollment);
    }

    @Transactional
    public EnrollmentResponse update(UUID id, EnrollmentRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula inexistente"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno inexistente"));

        ClassGroup classGroup = classGroupRepository.findById(request.getClassGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Turma inexistente"));

        validateStudioIdMatch(student, classGroup, request.getStudioId());
        validateStudentActiveForEnrollment(student);
        validateClassGroupActiveForEnrollment(classGroup);
        validateInstructorActiveForEnrollment(classGroup);
        validateEnrollmentDate(request.getEnrollmentDate());
        validateUniqueEnrollmentForUpdate(id, request.getStudentId(), request.getClassGroupId());

        if (!classGroup.getId().equals(enrollment.getClassGroup().getId())) {
            validateClassGroupCapacity(classGroup.getCapacity(), request.getStudioId(), request.getClassGroupId());
        }

        enrollment.setStudio(student.getStudio());
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
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula inexistente"));
        enrollmentRepository.delete(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findStudentsByClassGroupId(UUID classGroupId) {
        return enrollmentRepository.findByClassGroupIdAndActiveTrueAndStudentActiveTrueAndClassGroupActiveTrue(classGroupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void validateUniqueEnrollmentForUpdate(UUID enrollmentId, UUID studentId, UUID classGroupId) {
        enrollmentRepository.findByStudentIdAndClassGroupId(studentId, classGroupId)
                .ifPresent(existingEnrollment -> {
                    if (!existingEnrollment.getId().equals(enrollmentId)) {
                        throw new ConflictException("Aluno já matriculado");
                    }
                });
    }

    private void validateStudentActiveForEnrollment(Student student) {
        if (!Boolean.TRUE.equals(student.getActive())) {
            throw new ConflictException("Aluno inativo");
        }
    }

    private void validateClassGroupActiveForEnrollment(ClassGroup classGroup) {
        if (!Boolean.TRUE.equals(classGroup.getActive())) {
            throw new ConflictException("Turma inativa");
        }
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        ClassGroup classGroup = enrollment.getClassGroup();
        
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudio().getId(),
                student != null ? student.getId() : null,
                student != null ? student.getFullName() : null,
                classGroup != null ? classGroup.getId() : null,
                classGroup != null ? classGroup.getName() : null,
                enrollment.getEnrollmentDate(),
                enrollment.getActive(),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }
}
