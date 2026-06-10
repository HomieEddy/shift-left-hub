package com.shiftleft.hub.workspace.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for workspace data. */
public record WorkspaceResponse(
    UUID id,
    String name,
    String slug,
    String description,
    String logoUrl,
    long memberCount,
    UUID createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
