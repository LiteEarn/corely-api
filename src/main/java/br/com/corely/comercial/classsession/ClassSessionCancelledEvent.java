package br.com.corely.comercial.classsession;

import java.util.UUID;

public record ClassSessionCancelledEvent(Object source, UUID classSessionId) {
}
