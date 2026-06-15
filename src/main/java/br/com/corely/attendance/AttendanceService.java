package br.com.corely.attendance;

import br.com.corely.attendance.dto.AttendanceBulkRequest;
import br.com.corely.attendance.dto.AttendanceItemRequest;
import br.com.corely.attendance.dto.AttendanceRequest;
import br.com.corely.attendance.dto.AttendanceResponse;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
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

    @Transactional
    public AttendanceResponse create(AttendanceRequest request) {
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        ClassGroup classGroup = classGroupRepository.findById(request.getClassGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));

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
        return attendanceRepository.findByClassGroupId(classGroupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findByClassGroupIdAndAttendanceDate(UUID classGroupId, String attendanceDate) {
        java.time.LocalDate date = java.time.LocalDate.parse(attendanceDate);
        return attendanceRepository.findByClassGroupIdAndAttendanceDate(classGroupId, date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<AttendanceResponse> createBulk(AttendanceBulkRequest request) {
        Studio studio = studioRepository.findById(request.getStudioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        ClassGroup classGroup = classGroupRepository.findById(request.getClassGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));

        return request.getAttendances().stream()
                .map(itemRequest -> {
                    Student student = studentRepository.findById(itemRequest.getStudentId())
                            .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

                    attendanceRepository.findByStudentIdAndClassGroupIdAndAttendanceDate(
                                    itemRequest.getStudentId(), request.getClassGroupId(), request.getAttendanceDate())
                            .ifPresent(attendance -> {
                                throw new IllegalArgumentException("Attendance already exists for student " + 
                                        student.getFullName() + " on this date");
                            });

                    Attendance attendance = new Attendance();
                    attendance.setStudio(studio);
                    attendance.setStudent(student);
                    attendance.setClassGroup(classGroup);
                    attendance.setAttendanceDate(request.getAttendanceDate());
                    attendance.setPresent(itemRequest.getPresent());
                    attendance.setNotes(itemRequest.getNotes());

                    return toResponse(attendanceRepository.save(attendance));
                })
                .collect(Collectors.toList());
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
}
