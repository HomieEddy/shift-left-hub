package com.shiftleft.hub.workspace.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for workspace member data. */
public record WorkspaceUserResponse(
    UUID userId,
    String email,
    String displayName,
    String role,
    LocalDateTime joinedAt
) {
}
