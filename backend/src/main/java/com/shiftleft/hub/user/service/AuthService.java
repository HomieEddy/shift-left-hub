package com.shiftleft.hub.user.service;

import com.shiftleft.hub.common.DuplicateEmailException;
import com.shiftleft.hub.config.JwtService;
import com.shiftleft.hub.user.api.dto.AuthResponse;
import com.shiftleft.hub.user.api.dto.LoginRequest;
import com.shiftleft.hub.user.api.dto.RegisterRequest;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Register a new user with the given request.
     *
     * @param request the registration details
     * @return the auth response with tokens and user info
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(
                "Email already registered: " + request.email());
        }

        User user = User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .displayName(request.displayName())
            .role(UserRole.ROLE_USER)
            .enabled(true)
            .build();

        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Authenticate a user with email and password.
     *
     * @param request the login credentials
     * @return the auth response with tokens and user info
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!passwordEncoder.matches(
                request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Refresh an access token using a valid refresh token.
     *
     * @param refreshTokenValue the refresh token value
     * @return the auth response with new tokens
     */
    public AuthResponse refresh(String refreshTokenValue) {
        if (!jwtService.isTokenValid(refreshTokenValue)
                || !jwtService.isRefreshToken(refreshTokenValue)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        var userId = jwtService.extractUserId(refreshTokenValue);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BadCredentialsException("User not found"));

        String tokenId = jwtService.extractTokenId(refreshTokenValue);
        try {
            jwtService.validateRefreshRotation(tokenId, userId.toString());
        } catch (JwtException e) {
            throw new BadCredentialsException(e.getMessage());
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    /**
     * Log out a user by invalidating their refresh token.
     *
     * @param refreshTokenValue the refresh token value to invalidate
     */
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue != null
                && jwtService.isTokenValid(refreshTokenValue)) {
            try {
                String tokenId = jwtService.extractTokenId(refreshTokenValue);
                jwtService.invalidateRefreshToken(tokenId);
            } catch (Exception e) {
                // Token already invalid or malformed — no-op
            }
        }
    }

    /**
     * Switches the active workspace for a user by re-issuing tokens
     * with a new workspace_id claim.
     *
     * @param userId the user UUID
     * @param workspaceId the target workspace UUID
     * @return the auth response with new tokens
     */
    public AuthResponse switchWorkspace(UUID userId, UUID workspaceId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BadCredentialsException("User not found"));

        String accessToken = jwtService.generateAccessTokenWithWorkspace(user, workspaceId);
        String refreshToken = jwtService.generateRefreshTokenWithWorkspace(user, workspaceId);

        return buildAuthResponse(user, accessToken, refreshToken, workspaceId);
    }

    private AuthResponse buildAuthResponse(
            User user, String accessToken, String refreshToken) {
        return buildAuthResponse(user, accessToken, refreshToken, user.getDefaultWorkspaceId());
    }

    private AuthResponse buildAuthResponse(
            User user, String accessToken, String refreshToken, UUID effectiveWorkspaceId) {
        return new AuthResponse(
            accessToken,
            refreshToken,
            user.getId().toString(),
            user.getEmail(),
            user.getRole().name(),
            user.getDisplayName(),
            effectiveWorkspaceId != null ? effectiveWorkspaceId.toString() : null
        );
    }
}
