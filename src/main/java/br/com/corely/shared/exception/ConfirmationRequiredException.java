package br.com.corely.shared.exception;

import lombok.Getter;

@Getter
public class ConfirmationRequiredException extends RuntimeException {

    private final long activeEnrollments;

    public ConfirmationRequiredException(long activeEnrollments, String message) {
        super(message);
        this.activeEnrollments = activeEnrollments;
    }
}
