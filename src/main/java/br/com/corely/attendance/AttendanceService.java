package br.com.corely.attendance;

import br.com.corely.attendance.dto.AttendanceBulkRequest;
import br.com.corely.attendance.dto.AttendanceItemRequest;
import br.com.corely.attendance.dto.AttendanceRequest;
import br.com.corely.attendance.dto.AttendanceResponse;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.shared.exception.BusinessException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
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
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudioRepository studioRepository;
    private final StudentRepository studentRepository;
    private final ClassGroupRepository classGroupRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public AttendanceResponse create(AttendanceRequest request) {
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        ClassGroup classGroup = classGroupRepository.findById(request.getClassGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassGroupId(request.getStudentId(), request.getClassGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        validateStudentActiveForAttendance(student);
        validateEnrollmentActiveForAttendance(enrollment);
        validateClassGroupActiveForAttendance(classGroup);

        attendanceRepository.findByStudentIdAndClassGroupIdAndAttendanceDate(
                        request.getStudentId(), request.getClassGroupId(), request.getAttendanceDate())
                .ifPresent(attendance -> {
                    throw new IllegalArgumentException("Attendance already exists for this student, class group, and date");
                });

        Attendance attendance = new Attendance();
        attendance.setStudio(studio);
        attendance.setStudent(student);
        attendance.setClassGroup(classGroup);
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setPresent(request.getPresent());
        attendance.setNotes(request.getNotes());

        attendance = attendanceRepository.save(attendance);
        return toResponse(attendance);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findAll() {
        return attendanceRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AttendanceResponse findById(UUID id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
        return toResponse(attendance);
    }

    @Transactional
    public AttendanceResponse update(UUID id, AttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));

        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        ClassGroup classGroup = classGroupRepository.findById(request.getClassGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassGroupId(request.getStudentId(), request.getClassGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!student.getId().equals(attendance.getStudent().getId())) {
            validateStudentActiveForAttendance(student);
        }

        validateEnrollmentActiveForAttendance(enrollment);
        validateClassGroupActiveForAttendance(classGroup);

        attendanceRepository.findByStudentIdAndClassGroupIdAndAttendanceDate(
                        request.getStudentId(), request.getClassGroupId(), request.getAttendanceDate())
                .ifPresent(existingAttendance -> {
                    if (!existingAttendance.getId().equals(id)) {
                        throw new IllegalArgumentException("Attendance already exists for this student, class group, and date");
                    }
                });

        attendance.setStudio(studio);
        attendance.setStudent(student);
        attendance.setClassGroup(classGroup);
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setPresent(request.getPresent());
        attendance.setNotes(request.getNotes());

        attendance = attendanceRepository.save(attendance);
        return toResponse(attendance);
    }

    @Transactional
    public void delete(UUID id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));
        attendanceRepository.delete(attendance);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findByClassGroupId(UUID classGroupId) {
        return attendanceRepository.findByClassGroupIdAndStudentActiveTrueAndClassGroupActiveTrue(classGroupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findByClassGroupIdAndAttendanceDate(UUID classGroupId, String attendanceDate) {
        java.time.LocalDate date = java.time.LocalDate.parse(attendanceDate);

        // Primary source: active enrollments for the class group (with active students and active class group)
        List<Enrollment> enrollments = enrollmentRepository.findByClassGroupIdAndActiveTrueAndStudentActiveTrueAndClassGroupActiveTrue(classGroupId);

        return enrollments.stream()
                .map(enrollment -> {
                    // Check if attendance record exists for this student on the selected date
                    Attendance attendance = attendanceRepository.findByStudentIdAndClassGroupIdAndAttendanceDate(
                            enrollment.getStudent().getId(), classGroupId, date
                    ).orElse(null);

                    if (attendance != null) {
                        // Return existing attendance record
                        return toResponse(attendance);
                    } else {
                        // Return student with default attendance values (no attendance recorded yet)
                        return toResponseFromEnrollment(enrollment, classGroupId, date);
                    }
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public List<AttendanceResponse> createBulk(AttendanceBulkRequest request) {
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        ClassGroup classGroup = classGroupRepository.findById(request.getClassGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));

        validateClassGroupActiveForAttendance(classGroup);

        return request.getAttendances().stream()
                .map(itemRequest -> {
                    Student student = studentRepository.findById(itemRequest.getStudentId())
                            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

                    Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassGroupId(itemRequest.getStudentId(), request.getClassGroupId())
                            .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

                    validateStudentActiveForAttendance(student);
                    validateEnrollmentActiveForAttendance(enrollment);

                    // Check if attendance already exists - if so, update it instead of creating
                    Attendance attendance = attendanceRepository.findByStudentIdAndClassGroupIdAndAttendanceDate(
                                    itemRequest.getStudentId(), request.getClassGroupId(), request.getAttendanceDate())
                            .orElseGet(Attendance::new);

                    if (attendance.getId() == null) {
                        // New attendance - set all fields
                        attendance.setStudio(studio);
                        attendance.setStudent(student);
                        attendance.setClassGroup(classGroup);
                        attendance.setAttendanceDate(request.getAttendanceDate());
                    }
                    
                    // Update attendance fields (whether new or existing)
                    attendance.setPresent(itemRequest.getPresent());
                    attendance.setNotes(itemRequest.getNotes());

                    return toResponse(attendanceRepository.save(attendance));
                })
                .collect(Collectors.toList());
    }

    private void validateStudentActiveForAttendance(Student student) {
        if (!Boolean.TRUE.equals(student.getActive())) {
            throw new BusinessException("Aluno inativo.");
        }
    }

    private void validateEnrollmentActiveForAttendance(Enrollment enrollment) {
        if (!Boolean.TRUE.equals(enrollment.getActive())) {
            throw new BusinessException("Matrícula inativa.");
        }
    }

    private void validateClassGroupActiveForAttendance(ClassGroup classGroup) {
        if (!Boolean.TRUE.equals(classGroup.getActive())) {
            throw new BusinessException("The selected class group is inactive.");
        }
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getStudent().getId(),
                attendance.getStudent().getFullName(),
                attendance.getClassGroup().getId(),
                attendance.getClassGroup().getName(),
                attendance.getAttendanceDate(),
                attendance.getPresent(),
                attendance.getNotes(),
                attendance.getCreatedAt(),
                attendance.getUpdatedAt()
        );
    }

    private AttendanceResponse toResponseFromEnrollment(Enrollment enrollment, UUID classGroupId, java.time.LocalDate date) {
        return new AttendanceResponse(
                null, // No attendance ID yet
                enrollment.getStudent().getId(),
                enrollment.getStudent().getFullName(),
                classGroupId,
                enrollment.getClassGroup().getName(),
                date,
                false, // Default: not present (attendance not recorded yet)
                null, // No notes
                null, // No createdAt
                null  // No updatedAt
        );
    }
}
