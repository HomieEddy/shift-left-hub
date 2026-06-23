package com.shiftleft.hub.user.domain;

import java.util.UUID;

/**
 * Thrown when an admin attempts to modify their own role or enabled
 * status, which is a self-modification conflict.
 */
public class SelfModificationException extends RuntimeException {
    public SelfModificationException(String message) {
        super(message);
    }

    public static SelfModificationException role(UUID id) {
        return new SelfModificationException(
            "Cannot modify your own role (user " + id + ")");
    }

    public static SelfModificationException status(UUID id) {
        return new SelfModificationException(
            "Cannot modify your own account status (user " + id + ")");
    }
}
