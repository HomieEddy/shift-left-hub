package com.shiftleft.hub.user.service;

import com.shiftleft.hub.common.DuplicateEmailException;
import com.shiftleft.hub.config.JwtService;
import com.shiftleft.hub.user.api.dto.AuthResponse;
import com.shiftleft.hub.user.api.dto.LoginRequest;
import com.shiftleft.hub.user.api.dto.RegisterRequest;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private WorkspaceService workspaceService;

    @InjectMocks private AuthService authService;

    private final String email = "test@example.com";
    private final String password = "Password123!";
    private final String displayName = "Test User";
    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();

    private User createUser() {
        return User.builder()
            .id(userId)
            .email(email)
            .password("encoded-password")
            .displayName(displayName)
            .role(UserRole.ROLE_USER)
            .enabled(true)
            .defaultWorkspaceId(workspaceId)
            .build();
    }

    private Workspace createWorkspace() {
        return Workspace.builder()
            .id(workspaceId)
            .name("My Workspace")
            .slug("my-workspace")
            .createdBy(userId)
            .build();
    }

    @Test
    void register_shouldSucceedWhenEmailNotTaken() {
        RegisterRequest request = new RegisterRequest(email, password, displayName);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(createUser());
        when(workspaceService.createWorkspace(
            eq("My Workspace"), eq("Default personal workspace"), eq(null), eq(userId)))
            .thenReturn(createWorkspace());
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(email, response.email());
        assertEquals("access-token", response.accessToken());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void register_shouldThrowWhenEmailTaken() {
        RegisterRequest request = new RegisterRequest(email, password, displayName);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldSucceedWithValidCredentials() {
        LoginRequest request = new LoginRequest(email, password);
        User user = createUser();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(email, response.email());
    }

    @Test
    void login_shouldThrowWhenEmailNotFound() {
        LoginRequest request = new LoginRequest(email, password);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowWhenPasswordWrong() {
        LoginRequest request = new LoginRequest(email, "wrong-password");
        User user = createUser();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPassword())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowWhenUserDisabled() {
        LoginRequest request = new LoginRequest(email, password);
        User disabledUser = User.builder()
            .id(userId).email(email).password("encoded")
            .displayName(displayName).role(UserRole.ROLE_USER)
            .enabled(false)
            .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(disabledUser));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void refresh_shouldSucceedWithValidToken() {
        User user = createUser();
        when(jwtService.isTokenValid("valid-refresh-token")).thenReturn(true);
        when(jwtService.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-refresh-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.extractTokenId("valid-refresh-token")).thenReturn("token-id");
        doNothing().when(jwtService).validateRefreshRotation("token-id", userId.toString());
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");

        AuthResponse response = authService.refresh("valid-refresh-token");

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
    }

    @Test
    void refresh_shouldThrowWhenTokenInvalid() {
        when(jwtService.isTokenValid("invalid-token")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.refresh("invalid-token"));
    }

    @Test
    void logout_shouldInvalidateTokenWhenValid() {
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractTokenId("valid-token")).thenReturn("token-id");
        doNothing().when(jwtService).invalidateRefreshToken("token-id");

        authService.logout("valid-token");

        verify(jwtService).invalidateRefreshToken("token-id");
    }

    @Test
    void logout_shouldNotCallInvalidateWhenTokenNull() {
        authService.logout(null);
        verify(jwtService, never()).isTokenValid(any());
    }

    // ── register: validation ──────────────────────────────

    @Test
    void register_shouldThrowWhenEmailBlank() {
        RegisterRequest request = new RegisterRequest("", password, displayName);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldThrowWhenPasswordTooShort() {
        RegisterRequest request = new RegisterRequest(email, "Short1!", displayName);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldThrowWhenEmailNull() {
        LoginRequest request = new LoginRequest(null, password);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void refresh_shouldThrowWhenTokenExpired() {
        when(jwtService.isTokenValid("expired-token")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.refresh("expired-token"));
    }

    @Test
    void register_shouldSetCorrectDefaultRole() {
        RegisterRequest request = new RegisterRequest(email, password, displayName);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(createUser());
        when(workspaceService.createWorkspace(
            eq("My Workspace"), eq("Default personal workspace"), eq(null), eq(userId)))
            .thenReturn(createWorkspace());
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        authService.register(request);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());
        assertEquals(UserRole.ROLE_USER, captor.getValue().getRole());
    }

    @Test
    void logout_shouldNotThrowWhenExtractFails() {
        when(jwtService.isTokenValid("bad-token")).thenReturn(true);
        when(jwtService.extractTokenId("bad-token")).thenThrow(new RuntimeException("parse error"));

        assertDoesNotThrow(() -> authService.logout("bad-token"));
    }
}
