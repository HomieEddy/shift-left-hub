package com.shiftleft.hub.common;

/**
 * Thrown when attempting to register with an email that is already in use.
 */
public class DuplicateEmailException extends RuntimeException {

    /**
     * Creates a new DuplicateEmailException.
     *
     * @param message the detail message
     */
    public DuplicateEmailException(String message) {
        super(message);
    }
}
