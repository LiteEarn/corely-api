package br.com.corely.comercial.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class BulkAttendanceRequest {

    @NotNull
    private UUID classSessionId;

    @NotEmpty
    private @Valid List<AttendanceItem> attendances;

    public BulkAttendanceRequest() {}

    public BulkAttendanceRequest(UUID classSessionId, List<AttendanceItem> attendances) {
        this.classSessionId = classSessionId;
        this.attendances = attendances;
    }

    public UUID getClassSessionId() { return classSessionId; }
    public void setClassSessionId(UUID classSessionId) { this.classSessionId = classSessionId; }
    public List<AttendanceItem> getAttendances() { return attendances; }
    public void setAttendances(List<AttendanceItem> attendances) { this.attendances = attendances; }

    public static class AttendanceItem {
        @NotNull
        private UUID bookingId;
        private boolean present;
        private String notes;

        public AttendanceItem() {}

        public AttendanceItem(UUID bookingId, boolean present, String notes) {
            this.bookingId = bookingId;
            this.present = present;
            this.notes = notes;
        }

        public UUID getBookingId() { return bookingId; }
        public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
        public boolean isPresent() { return present; }
        public void setPresent(boolean present) { this.present = present; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
