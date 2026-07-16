package br.com.corely.comercial.classsession;

import java.util.UUID;

public record ClassSessionFinishedEvent(Object source, UUID classSessionId) {
}
