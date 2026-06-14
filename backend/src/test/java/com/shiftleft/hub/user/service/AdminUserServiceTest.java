package com.shiftleft.hub.user.service;

import com.shiftleft.hub.user.api.dto.UserResponse;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserNotFoundException;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private AdminUserService adminUserService;

    private final UUID adminId = UUID.randomUUID();
    private final UUID targetUserId = UUID.randomUUID();
    private final String adminEmail = "admin@example.com";

    private User createAdmin() {
        return User.builder()
            .id(adminId)
            .email(adminEmail)
            .password("encoded")
            .displayName("Admin")
            .role(UserRole.ROLE_ADMIN)
            .enabled(true)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private User createTargetUser() {
        return User.builder()
            .id(targetUserId)
            .email("user@example.com")
            .password("encoded")
            .displayName("Target User")
            .role(UserRole.ROLE_USER)
            .enabled(true)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private void mockSecurityContext() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(adminEmail);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    // ── getAllUsers ───────────────────────────────────────────

    @Test
    void listUsers_shouldReturnAllUsers() {
        User user1 = createAdmin();
        User user2 = createTargetUser();
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> result = adminUserService.getAllUsers();

        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void listUsers_shouldReturnEmptyListWhenNoneExist() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> result = adminUserService.getAllUsers();

        assertTrue(result.isEmpty());
    }

    // ── getUserById ───────────────────────────────────────────

    @Test
    void getUserById_shouldSucceed() {
        User user = createTargetUser();
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));

        UserResponse result = adminUserService.getUserById(targetUserId);

        assertEquals(targetUserId, result.id());
        assertEquals("user@example.com", result.email());
    }

    @Test
    void getUserById_shouldThrowWhenNotFound() {
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminUserService.getUserById(missingId));
    }

    // ── updateUserRole ────────────────────────────────────────

    @Test
    void updateUserRole_shouldSucceed() {
        mockSecurityContext();
        User targetUser = createTargetUser();
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        UserResponse result = adminUserService.updateUserRole(targetUserId, UserRole.ROLE_AGENT);

        assertEquals(UserRole.ROLE_AGENT, result.role());
        verify(userRepository).save(targetUser);
    }

    @Test
    void updateUserRole_shouldThrowWhenSelfDemote() {
        mockSecurityContext();
        User adminUser = createAdmin();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalStateException.class,
            () -> adminUserService.updateUserRole(adminId, UserRole.ROLE_USER));
        verify(userRepository, never()).save(any());
    }

    // ── toggleUserStatus ──────────────────────────────────────

    @Test
    void toggleUserEnabled_shouldEnableDisableUser() {
        mockSecurityContext();
        User targetUser = createTargetUser();
        targetUser.setEnabled(false);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        UserResponse result = adminUserService.toggleUserStatus(targetUserId);

        assertTrue(result.enabled());
        verify(userRepository).save(targetUser);
    }

    @Test
    void toggleUserEnabled_shouldThrowWhenSelfDisable() {
        mockSecurityContext();
        User adminUser = createAdmin();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalStateException.class,
            () -> adminUserService.toggleUserStatus(adminId));
        verify(userRepository, never()).save(any());
    }
}
