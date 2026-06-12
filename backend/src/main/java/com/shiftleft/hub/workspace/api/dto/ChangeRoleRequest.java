package com.shiftleft.hub.workspace.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeRoleRequest(
    @NotBlank String role
) {
}
