package br.com.corely.comercial.classsession.dto;

import br.com.corely.comercial.classsession.SessionCancelReason;
import jakarta.validation.constraints.NotNull;

public class CancelSessionRequest {

    @NotNull
    private SessionCancelReason reason;

    private String description;

    public CancelSessionRequest() {}

    public SessionCancelReason getReason() { return reason; }
    public void setReason(SessionCancelReason reason) { this.reason = reason; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
