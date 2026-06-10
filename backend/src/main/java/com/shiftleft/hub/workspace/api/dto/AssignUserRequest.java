package com.shiftleft.hub.workspace.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Request DTO for assigning a user to a workspace. */
public record AssignUserRequest(
    @NotNull UUID userId,
    @NotBlank String role
) {
}
