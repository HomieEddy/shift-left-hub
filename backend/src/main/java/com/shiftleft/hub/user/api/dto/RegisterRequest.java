package com.shiftleft.hub.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Password must be at least 8 characters with at least 1 uppercase letter and 1 digit")
        String password,
        @NotBlank String displayName
) {}
