package com.shiftleft.hub.workspace.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeRoleRequest(
    @NotBlank
    @Pattern(regexp = "^(ADMIN|MEMBER|READ_ONLY)$", message = "Role must be ADMIN, MEMBER, or READ_ONLY")
    String role
) {
}
