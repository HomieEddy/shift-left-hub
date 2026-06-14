package com.shiftleft.hub.workspace.domain;

import java.util.UUID;

/**
 * Thrown when a workspace is not found by its identifier.
 * <p>Mapped to a 404 response by the global {@code @RestControllerAdvice} handler.</p>
 */
public class WorkspaceNotFoundException extends RuntimeException {

    public WorkspaceNotFoundException(UUID id) {
        super("Workspace not found: " + id);
    }

    public WorkspaceNotFoundException(String message) {
        super(message);
    }
}
