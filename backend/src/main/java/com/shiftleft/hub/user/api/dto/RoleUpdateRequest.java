package com.shiftleft.hub.user.api.dto;

import com.shiftleft.hub.user.domain.UserRole;
import jakarta.validation.constraints.NotNull;

/** Request payload for updating a user's role. */
public record RoleUpdateRequest(
    @NotNull UserRole role
) {
}
