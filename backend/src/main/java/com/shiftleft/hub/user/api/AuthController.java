package com.shiftleft.hub.user.api;

import com.shiftleft.hub.config.AuthCookieProperties;
import com.shiftleft.hub.user.api.dto.AuthResponse;
import com.shiftleft.hub.user.api.dto.LoginRequest;
import com.shiftleft.hub.user.api.dto.RegisterRequest;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.service.AuthService;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final int ACCESS_COOKIE_MAX_AGE = 3600;
    private static final int REFRESH_COOKIE_MAX_AGE = 604800;

    private final AuthService authService;
    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;
    private final AuthCookieProperties cookieProperties;

    /**
     * Register a new user.
     *
     * @param request the registration details
     * @return auth response with tokens
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, createAccessCookie(response.accessToken()).toString())
            .header(HttpHeaders.SET_COOKIE, createRefreshCookie(response.refreshToken()).toString())
            .body(response);
    }

    /**
     * Authenticate with email/password.
     *
     * @param request the login credentials
     * @return auth response with tokens
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, createAccessCookie(response.accessToken()).toString())
            .header(HttpHeaders.SET_COOKIE, createRefreshCookie(response.refreshToken()).toString())
            .body(response);
    }

    /**
     * Refresh an access token using the refresh cookie.
     *
     * @param refreshToken the refresh token value
     * @return auth response with new tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue("refresh_token") String refreshToken) {
        AuthResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, createAccessCookie(response.accessToken()).toString())
            .header(HttpHeaders.SET_COOKIE, createRefreshCookie(response.refreshToken()).toString())
            .body(response);
    }

    /**
     * Log out and clear session cookies.
     *
     * @param refreshToken the refresh token to invalidate
     * @return success message
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(value = "refresh_token", defaultValue = "") String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, clearCookie("access_token").toString())
            .header(HttpHeaders.SET_COOKIE, clearCookie("refresh_token").toString())
            .body(Map.of("message", "Logged out"));
    }

    /**
     * Switch active workspace by re-issuing JWT with new workspace_id claim.
     *
     * @param id the target workspace UUID
     * @param userDetails the authenticated user
     * @return auth response with new tokens
     */
    @PostMapping("/workspace/{id}")
    public ResponseEntity<AuthResponse> switchWorkspace(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        if (!workspaceService.isMemberOfWorkspace(id, user.getId())) {
            throw new IllegalArgumentException("User is not a member of this workspace");
        }
        AuthResponse response = authService.switchWorkspace(user.getId(), id);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, createAccessCookie(response.accessToken()).toString())
            .header(HttpHeaders.SET_COOKIE, createRefreshCookie(response.refreshToken()).toString())
            .body(response);
    }

    private ResponseCookie createAccessCookie(String token) {
        return ResponseCookie.from("access_token", token)
            .httpOnly(true).secure(cookieProperties.isSecure()).sameSite(cookieProperties.getSameSite())
            .path("/").maxAge(ACCESS_COOKIE_MAX_AGE).build();
    }

    private ResponseCookie createRefreshCookie(String token) {
        return ResponseCookie.from("refresh_token", token)
            .httpOnly(true).secure(cookieProperties.isSecure()).sameSite(cookieProperties.getSameSite())
            .path("/").maxAge(REFRESH_COOKIE_MAX_AGE).build();
    }

    private ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "").httpOnly(true).secure(cookieProperties.isSecure())
            .sameSite(cookieProperties.getSameSite()).path("/").maxAge(0).build();
    }
}
