package com.shiftleft.hub.workspace.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when a workspace invitation is not found by its identifier.
 * <p>Mapped to a 404 response by the global {@code @RestControllerAdvice} handler.</p>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException(UUID id) {
        super("Invitation not found: " + id);
    }

    public InvitationNotFoundException(String message) {
        super(message);
    }
}
