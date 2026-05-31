package com.shiftleft.hub.user.api.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String userId,
        String email,
        String role,
        String displayName
) {}
