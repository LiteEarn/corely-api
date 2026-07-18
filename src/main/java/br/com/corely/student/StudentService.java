package br.com.corely.student;

import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.finance.membershipplan.MembershipPlanRepository;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.dto.StudentRequest;
import br.com.corely.student.dto.StudentResponse;
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
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudioRepository studioRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MembershipPlanRepository membershipPlanRepository;

    @Transactional
    public StudentResponse create(StudentRequest request) {
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        var membershipPlan = membershipPlanRepository.findById(request.getMembershipPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Membership plan not found"));

        Student student = new Student();
        student.setStudio(studio);
        student.setFullName(request.getFullName());
        student.setPhone(request.getPhone());
        student.setEmail(request.getEmail());
        student.setBirthDate(request.getBirthDate());
        student.setActive(true);
        student.setBillingEnabled(true);
        student.setMembershipPlan(membershipPlan);

        student = studentRepository.save(student);
        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> findAll() {
        return studentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(UUID id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        return toResponse(student);
    }

    @Transactional
    public StudentResponse update(UUID id, StudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        student.setStudio(studio);
        student.setFullName(request.getFullName());
        student.setPhone(request.getPhone());
        student.setEmail(request.getEmail());
        student.setBirthDate(request.getBirthDate());
        if (request.getActive() != null) {
            if (Boolean.TRUE.equals(student.getActive()) && Boolean.FALSE.equals(request.getActive())) {
                deactivateActiveEnrollments(id);
            }
            student.setActive(request.getActive());
        }

        if (request.getBillingEnabled() != null) {
            student.setBillingEnabled(request.getBillingEnabled());
        }

        if (request.getMembershipPlanId() != null) {
            var membershipPlan = membershipPlanRepository.findById(request.getMembershipPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Membership plan not found"));
            student.setMembershipPlan(membershipPlan);
        }

        student = studentRepository.save(student);
        return toResponse(student);
    }

    @Transactional
    public void delete(UUID id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        studentRepository.delete(student);
    }

    private void deactivateActiveEnrollments(UUID studentId) {
        List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentIdAndActiveTrue(studentId);
        for (Enrollment enrollment : activeEnrollments) {
            enrollment.setActive(false);
        }
        enrollmentRepository.saveAll(activeEnrollments);
    }

    private StudentResponse toResponse(Student student) {
        UUID planId = student.getMembershipPlan() != null ? student.getMembershipPlan().getId() : null;
        String planName = student.getMembershipPlan() != null ? student.getMembershipPlan().getName() : null;
        return new StudentResponse(
                student.getId(),
                student.getStudio().getId(),
                student.getFullName(),
                student.getPhone(),
                student.getEmail(),
                student.getBirthDate(),
                student.getActive(),
                student.getBillingEnabled(),
                planId,
                planName
        );
    }
}
