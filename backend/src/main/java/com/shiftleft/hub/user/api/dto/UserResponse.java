package com.shiftleft.hub.user.api.dto;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String displayName,
    UserRole role,
    boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getRole(),
            user.isEnabled(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
