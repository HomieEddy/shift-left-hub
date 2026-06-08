package com.shiftleft.hub.user.api.dto;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

/** Response containing user details for admin operations. */
public record UserResponse(
    UUID id,
    String email,
    String displayName,
    UserRole role,
    boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    /**
     * Create a UserResponse from a User entity.
     *
     * @param user the user entity
     * @return the user response
     */
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
