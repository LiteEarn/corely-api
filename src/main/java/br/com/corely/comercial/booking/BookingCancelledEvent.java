package br.com.corely.comercial.booking;

import java.util.UUID;

public record BookingCancelledEvent(Object source, UUID classSessionId) {
}
