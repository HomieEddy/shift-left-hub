package com.shiftleft.hub.workspace.api.dto;

import com.shiftleft.hub.workspace.domain.Workspace;

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
    /**
     * Build a response DTO from a {@link Workspace} entity and a
     * pre-computed member count.
     *
     * <p>The repository layer is expected to provide the count via a
     * bulk query (see {@code WorkspaceRepository.countMembersByIds}) so
     * that list endpoints avoid N+1 lookups.
     */
    public static WorkspaceResponse from(Workspace workspace, long memberCount) {
        return new WorkspaceResponse(
            workspace.getId(),
            workspace.getName(),
            workspace.getSlug(),
            workspace.getDescription(),
            workspace.getLogoUrl(),
            workspace.getIcon(),
            memberCount,
            workspace.getCreatedBy(),
            workspace.getCreatedAt(),
            workspace.getUpdatedAt()
        );
    }
}
