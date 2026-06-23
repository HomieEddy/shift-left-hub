package com.shiftleft.hub.common.config;

import com.shiftleft.hub.user.domain.AdminNotFoundException;
import com.shiftleft.hub.workspace.domain.InvitationNotFoundException;
import com.shiftleft.hub.workspace.service.LastAdminException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the 3 new exception handlers added in Tier 13:
 * - {@link AdminNotFoundException} → 401
 * - {@link InvitationNotFoundException} → 404
 * - {@link LastAdminException} → 409
 *
 * The general handlers (UserNotFoundException, ArticleNotFoundException, etc.)
 * follow the same shape and are exercised by integration tests.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        Environment env = mock(Environment.class);
        handler = new GlobalExceptionHandler(env);
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void adminNotFound_mapsTo401() {
        ProblemDetail pd = handler.handleAdminNotFound(new AdminNotFoundException(), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(pd.getTitle()).isEqualTo("Authentication Error");
        assertThat(pd.getDetail()).contains("admin user could not be resolved");
    }

    @Test
    void invitationNotFound_mapsTo404() {
        ProblemDetail pd = handler.handleInvitationNotFound(
            new InvitationNotFoundException(java.util.UUID.randomUUID()), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Entity Not Found");
        assertThat(pd.getDetail()).contains("Invitation not found");
    }

    @Test
    void lastAdmin_mapsTo409() {
        ProblemDetail pd = handler.handleLastAdmin(
            new LastAdminException("Cannot remove the only admin"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pd.getTitle()).isEqualTo("Workspace Conflict");
        assertThat(pd.getDetail()).isEqualTo("Cannot remove the only admin");
    }
}
