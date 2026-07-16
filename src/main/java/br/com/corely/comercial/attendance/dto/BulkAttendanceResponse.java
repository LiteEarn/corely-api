package br.com.corely.comercial.attendance.dto;

public class BulkAttendanceResponse {

    private String message;
    private int savedCount;

    public BulkAttendanceResponse() {}

    public BulkAttendanceResponse(String message, int savedCount) {
        this.message = message;
        this.savedCount = savedCount;
    }

    public String getMessage() { return message; }
    public int getSavedCount() { return savedCount; }
}
