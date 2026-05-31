package com.shiftleft.hub.user.api.dto;

import com.shiftleft.hub.user.domain.UserRole;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(
    @NotNull UserRole role
) {}
