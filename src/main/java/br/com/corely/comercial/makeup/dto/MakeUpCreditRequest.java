package br.com.corely.comercial.makeup.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class MakeUpCreditRequest {

    @NotNull
    private UUID classSessionId;

    public MakeUpCreditRequest() {}

    public UUID getClassSessionId() { return classSessionId; }
    public void setClassSessionId(UUID classSessionId) { this.classSessionId = classSessionId; }
}
