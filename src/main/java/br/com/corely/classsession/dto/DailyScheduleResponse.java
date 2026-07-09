package br.com.corely.classsession.dto;

import br.com.corely.classsession.ClassSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class DailyScheduleResponse {
    private DailyKpis kpis;
    private List<DailySessionItem> sessions;

    @Data
    @AllArgsConstructor
    public static class DailyKpis {
        private long totalToday;
        private long inProgress;
        private long completed;
        private long cancelled;
    }

    @Data
    @AllArgsConstructor
    public static class DailySessionItem {
        private UUID id;
        private UUID classGroupId;
        private String classGroupName;
        private UUID instructorId;
        private String instructorName;
        private LocalDate sessionDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private ClassSessionStatus status;
        private Integer capacity;
        private long enrolledCount;
        private long presentCount;
        private String notes;
    }
}