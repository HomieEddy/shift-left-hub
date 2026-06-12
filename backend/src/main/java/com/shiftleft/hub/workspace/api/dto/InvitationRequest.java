package com.shiftleft.hub.workspace.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InvitationRequest(
    @NotNull UUID userId,
    @NotBlank String role
) {
}
