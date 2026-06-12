package com.shiftleft.hub.workspace.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceResponse(
    UUID id,
    String name,
    String slug,
    String description,
    String logoUrl,
    String icon,
    long memberCount,
    UUID createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
