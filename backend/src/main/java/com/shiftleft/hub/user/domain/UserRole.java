package com.shiftleft.hub.user.domain;

/**
 * Enumeration of user roles in the system.
 * <p>Each role grants a distinct set of permissions. {@code ROLE_USER}
 * is the default role for end users submitting tickets. {@code ROLE_AGENT}
 * grants access to agent ticket workflows. {@code ROLE_ADMIN} provides
 * system administration privileges.</p>
 */
public enum UserRole {
    /** Standard end-user who can submit and track tickets. */
    ROLE_USER,
    /** Support agent who can claim, work on, and resolve tickets. */
    ROLE_AGENT,
    /** System administrator with elevated privileges. */
    ROLE_ADMIN
}
