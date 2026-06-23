package com.shiftleft.hub.workspace.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a workspace operation would leave it with zero administrators.
 * <p>Distinct from a generic validation error (400) because it is a domain
 * conflict — the request is well-formed but the resulting state is invalid.
 * Mapped to a 409 Conflict response by the global {@code @RestControllerAdvice}
 * handler.</p>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class LastAdminException extends RuntimeException {

    public LastAdminException(String message) {
        super(message);
    }
}
