package br.com.corely.makeup;

import br.com.corely.attendance.Attendance;
import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.attendance.AttendanceStatus;
import br.com.corely.makeup.dto.MakeupRequestRequest;
import br.com.corely.makeup.dto.MakeupRequestResponse;
import br.com.corely.shared.exception.ConflictException;
import br.com.corely.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MakeupRequestService {

    private final MakeupRequestRepository makeupRequestRepository;
    private final AttendanceRepository attendanceRepository;

    @Transactional
    public MakeupRequestResponse request(UUID attendanceId, MakeupRequestRequest request) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found"));

        if (attendance.getStatus() != AttendanceStatus.ABSENT) {
            throw new ConflictException(
                    "Makeup request is only allowed for absent attendances. Current status: " + attendance.getStatus()
            );
        }

        if (makeupRequestRepository.existsByAttendanceId(attendanceId)) {
            throw new ConflictException("A makeup request already exists for this attendance");
        }

        MakeupRequest makeupRequest = new MakeupRequest();
        makeupRequest.setAttendance(attendance);
        makeupRequest.setStatus(MakeupRequestStatus.REQUESTED);
        makeupRequest.setReason(request.getReason());
        makeupRequest.setRequestedAt(LocalDateTime.now());

        makeupRequest = makeupRequestRepository.save(makeupRequest);
        return toResponse(makeupRequest);
    }

    @Transactional(readOnly = true)
    public MakeupRequestResponse findByAttendanceId(UUID attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new ResourceNotFoundException("Attendance not found");
        }

        return makeupRequestRepository.findByAttendanceId(attendanceId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Makeup request not found for this attendance"));
    }

    @Transactional(readOnly = true)
    public List<MakeupRequestResponse> findAll(MakeupRequestStatus status, UUID studentId, UUID classGroupId) {
        if (status != null) {
            return makeupRequestRepository.findByStatus(status).stream()
                    .map(this::toResponse)
                    .toList();
        }

        return makeupRequestRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private MakeupRequestResponse toResponse(MakeupRequest makeupRequest) {
        return new MakeupRequestResponse(
                makeupRequest.getId(),
                makeupRequest.getAttendance().getId(),
                makeupRequest.getTargetSession() != null ? makeupRequest.getTargetSession().getId() : null,
                makeupRequest.getStatus(),
                makeupRequest.getReason(),
                makeupRequest.getRequestedAt(),
                makeupRequest.getApprovedAt(),
                makeupRequest.getCreatedAt(),
                makeupRequest.getUpdatedAt()
        );
    }
}
