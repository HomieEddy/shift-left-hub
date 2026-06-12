package com.shiftleft.hub.workspace.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceInvitationResponse(
    UUID id,
    UUID workspaceId,
    UUID invitedUserId,
    String invitedUserEmail,
    String invitedUserDisplayName,
    UUID invitedBy,
    String role,
    String status,
    LocalDateTime createdAt
) {
}
