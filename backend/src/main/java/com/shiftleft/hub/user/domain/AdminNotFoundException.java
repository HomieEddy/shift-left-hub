package com.shiftleft.hub.user.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the authenticated admin user cannot be resolved from the security
 * context. This should never happen in production (Spring Security guarantees
 * a valid {@code UserDetails} for any authenticated request on these endpoints)
 * but is needed so controllers don't leak raw {@code RuntimeException}s into
 * the global handler — which would surface as an opaque 500.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AdminNotFoundException extends RuntimeException {

    public AdminNotFoundException() {
        super("Authenticated admin user could not be resolved");
    }

    public AdminNotFoundException(String message) {
        super(message);
    }
}
