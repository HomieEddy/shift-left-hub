package com.shiftleft.hub.auth.integration;

import com.shiftleft.hub.AbstractIntegrationTest;
import com.shiftleft.hub.user.api.dto.AuthResponse;
import com.shiftleft.hub.user.api.dto.LoginRequest;
import com.shiftleft.hub.user.api.dto.RegisterRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test covering the full auth lifecycle:
 * register → login → access protected resource → refresh → logout → replay rejection.
 * <p>All tests run against a real pgvector PostgreSQL container via Testcontainers,
 * with the full Spring Boot application context loaded.</p>
 *
 * @see AbstractIntegrationTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.MethodName.class)
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    private static String accessToken;
    private static String refreshToken;
    private static String consumedRefreshToken;

    private static final String EMAIL = "auth-flow@shiftleft.local";
    private static final String PASSWORD = "TestPass1";
    private static final String DISPLAY_NAME = "Auth Flow User";

    private WebTestClient client() {
        if (webTestClient == null) {
            webTestClient = WebTestClient.bindToServer()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return webTestClient;
    }

    @Test
    void t01_register_shouldCreateUserAndReturnTokens() {
        var request = new RegisterRequest(EMAIL, PASSWORD, DISPLAY_NAME);

        AuthResponse response = client().post().uri("/api/auth/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.role()).isEqualTo("ROLE_USER");
        assertThat(response.displayName()).isEqualTo(DISPLAY_NAME);

        accessToken = response.accessToken();
        refreshToken = response.refreshToken();
    }

    @Test
    void t02_login_shouldAuthenticateAndReturnNewTokens() {
        var request = new LoginRequest(EMAIL, PASSWORD);

        var result = client().post().uri("/api/auth/login")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult();

        AuthResponse response = result.getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(result.getResponseHeaders().get(HttpHeaders.SET_COOKIE))
                .isNotNull()
                .anySatisfy(cookie -> {
                    assertThat(cookie).contains("access_token=");
                    assertThat(cookie).contains("SameSite=Lax");
                    assertThat(cookie).doesNotContain("Secure");
                });

        accessToken = response.accessToken();
        refreshToken = response.refreshToken();
    }

    @Test
    void t03_accessProtectedEndpoint_shouldSucceedWithAccessToken() {
        client().get().uri("/api/tickets")
                .cookie("access_token", accessToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void t03a_accessProtectedEndpoint_shouldSucceedWithBearerToken() {
        client().get().uri("/api/tickets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void t03b_accessProtectedEndpoint_shouldReturnUnauthorizedWithoutAccessToken() {
        client().get().uri("/api/tickets")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void t04_refreshToken_shouldReturnNewTokensAndOldAccessStillWorks() {
        // Store the pre-refresh refresh token for the replay test
        consumedRefreshToken = refreshToken;

        AuthResponse response = client().post().uri("/api/auth/refresh")
                .cookie("refresh_token", refreshToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();

        // Verify new access token works on a protected endpoint
        client().get().uri("/api/tickets")
                .cookie("access_token", response.accessToken())
                .exchange()
                .expectStatus().isOk();

        // Update static state for subsequent tests
        accessToken = response.accessToken();
        refreshToken = response.refreshToken();
    }

    @Test
    void t05_logout_shouldInvalidateRefreshToken() {
        client().post().uri("/api/auth/logout")
                .cookie("refresh_token", refreshToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void t06_replayConsumedRefreshToken_shouldBeRejected() {
        client().post().uri("/api/auth/refresh")
                .cookie("refresh_token", consumedRefreshToken)
                .exchange()
                .expectStatus().is4xxClientError();
    }
}
