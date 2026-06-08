package com.shiftleft.hub.user.api;

import com.shiftleft.hub.user.api.dto.AuthResponse;
import com.shiftleft.hub.user.api.dto.LoginRequest;
import com.shiftleft.hub.user.api.dto.RegisterRequest;
import com.shiftleft.hub.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final int ACCESS_COOKIE_MAX_AGE = 900;
    private static final int REFRESH_COOKIE_MAX_AGE = 604800;

    private final AuthService authService;

    /**
     * Register a new user with the given request.
     *
     * @param request the registration details
     * @return the auth response with tokens and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE,
                createAccessCookie(response.accessToken()).toString())
            .header(HttpHeaders.SET_COOKIE,
                createRefreshCookie(response.refreshToken()).toString())
            .body(response);
    }

    /**
     * Authenticate a user with email and password.
     *
     * @param request the login credentials
     * @return the auth response with tokens and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE,
                createAccessCookie(response.accessToken()).toString())
            .header(HttpHeaders.SET_COOKIE,
                createRefreshCookie(response.refreshToken()).toString())
            .body(response);
    }

    /**
     * Refresh an access token using a valid refresh token.
     *
     * @param refreshToken the refresh token value
     * @return the auth response with new tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue("refresh_token") String refreshToken) {
        AuthResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE,
                createAccessCookie(response.accessToken()).toString())
            .header(HttpHeaders.SET_COOKIE,
                createRefreshCookie(response.refreshToken()).toString())
            .body(response);
    }

    /**
     * Log out a user by clearing their refresh token.
     *
     * @param refreshToken the refresh token value to invalidate
     * @return a success message
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(value = "refresh_token",
                    defaultValue = "") String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, clearCookie("access_token").toString())
            .header(HttpHeaders.SET_COOKIE, clearCookie("refresh_token").toString())
            .body(Map.of("message", "Logged out"));
    }

    private ResponseCookie createAccessCookie(String token) {
        return ResponseCookie.from("access_token", token)
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(ACCESS_COOKIE_MAX_AGE)
            .build();
    }

    private ResponseCookie createRefreshCookie(String token) {
        return ResponseCookie.from("refresh_token", token)
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(REFRESH_COOKIE_MAX_AGE)
            .build();
    }

    private ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(0)
            .build();
    }
}
