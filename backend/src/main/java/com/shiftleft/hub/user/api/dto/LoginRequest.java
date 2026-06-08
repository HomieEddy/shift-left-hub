package com.shiftleft.hub.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Login credentials for authentication. */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
