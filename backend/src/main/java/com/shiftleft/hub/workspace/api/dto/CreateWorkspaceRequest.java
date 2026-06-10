package com.shiftleft.hub.workspace.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request DTO for creating a new workspace. */
public record CreateWorkspaceRequest(
    @NotBlank @Size(max = 255) String name,
    @Size(max = 1000) String description,
    @Size(max = 512) String logoUrl
) {
}
