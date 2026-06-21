package com.shiftleft.hub.common.domain;

import java.util.UUID;

/**
 * Request-scoped holder for the active workspace ID.
 * Set once per request during JWT authentication.
 * Used by WorkspaceFilterAspect to enable the Hibernate workspace @Filter.
 */
public final class WorkspaceContextHolder {

    private static final ThreadLocal<UUID> currentWorkspaceId = new ThreadLocal<>();

    private WorkspaceContextHolder() {
    }

    /**
     * Sets the current workspace ID for this request.
     *
     * @param workspaceId the workspace UUID to associate with the current request
     */
    public static void setCurrentWorkspaceId(UUID workspaceId) {
        currentWorkspaceId.set(workspaceId);
    }

    /**
     * Returns the current workspace ID, or null if none is set.
     *
     * <p>Use this in code paths that need to optionally restore a previously
     * set context (e.g. admin operations that temporarily clear the
     * workspace filter to run a cross-workspace query).</p>
     *
     * @return the workspace UUID, or null
     */
    public static UUID getCurrentWorkspaceIdOrNull() {
        return currentWorkspaceId.get();
    }

    /**
     * Returns the current workspace ID.
     *
     * @return the workspace UUID for the current request
     * @throws IllegalStateException if no workspace is set in the current context
     */
    public static UUID getCurrentWorkspaceId() {
        UUID id = currentWorkspaceId.get();
        if (id == null) {
            throw new IllegalStateException("No active workspace in current request context");
        }
        return id;
    }

    /**
     * Checks whether a workspace ID is set in the current context.
     *
     * @return true if a workspace ID is available
     */
    public static boolean hasCurrentWorkspaceId() {
        return currentWorkspaceId.get() != null;
    }

    /** Clears the current workspace ID. Called in filter finally blocks. */
    public static void clear() {
        currentWorkspaceId.remove();
    }
}
