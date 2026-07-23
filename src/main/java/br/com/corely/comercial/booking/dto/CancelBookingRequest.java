package br.com.corely.comercial.booking.dto;

import br.com.corely.comercial.booking.CancelReason;

public class CancelBookingRequest {

    private CancelReason reason;
    private String description;

    public CancelBookingRequest() {}

    public CancelBookingRequest(CancelReason reason, String description) {
        this.reason = reason;
        this.description = description;
    }

    public CancelReason getReason() { return reason; }
    public void setReason(CancelReason reason) { this.reason = reason; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
