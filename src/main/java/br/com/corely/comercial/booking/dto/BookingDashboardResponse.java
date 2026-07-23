package br.com.corely.comercial.booking.dto;

import java.time.LocalDate;

public class BookingDashboardResponse {

    private LocalDate date;
    private long totalBookings;
    private long confirmed;
    private long cancelled;
    private long noShow;
    private long completed;
    private int totalCapacity;
    private int totalBooked;
    private int freeCapacity;
    private double occupancyRate;

    public BookingDashboardResponse() {}

    public BookingDashboardResponse(LocalDate date, long totalBookings, long confirmed, long cancelled,
                                    long noShow, long completed, int totalCapacity, int totalBooked,
                                    int freeCapacity, double occupancyRate) {
        this.date = date;
        this.totalBookings = totalBookings;
        this.confirmed = confirmed;
        this.cancelled = cancelled;
        this.noShow = noShow;
        this.completed = completed;
        this.totalCapacity = totalCapacity;
        this.totalBooked = totalBooked;
        this.freeCapacity = freeCapacity;
        this.occupancyRate = occupancyRate;
    }

    public LocalDate getDate() { return date; }
    public long getTotalBookings() { return totalBookings; }
    public long getConfirmed() { return confirmed; }
    public long getCancelled() { return cancelled; }
    public long getNoShow() { return noShow; }
    public long getCompleted() { return completed; }
    public int getTotalCapacity() { return totalCapacity; }
    public int getTotalBooked() { return totalBooked; }
    public int getFreeCapacity() { return freeCapacity; }
    public double getOccupancyRate() { return occupancyRate; }
}
