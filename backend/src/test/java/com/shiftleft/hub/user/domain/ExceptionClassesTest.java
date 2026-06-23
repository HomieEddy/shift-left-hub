package com.shiftleft.hub.user.domain;

import com.shiftleft.hub.workspace.domain.InvitationNotFoundException;
import com.shiftleft.hub.workspace.service.LastAdminException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Direct unit tests for the 3 exception classes introduced in Tier 13.
 * <p>The exceptions are simple typed wrappers, but SonarCloud measures
 * coverage on new code and a 0% direct coverage on these classes
 * dropped the PR's quality gate from passed to failed (required ≥ 80%
 * on new code per project policy).</p>
 * <p>Indirect coverage exists via {@code GlobalExceptionHandlerTest} which
 * exercises the handler logic, but Sonar counts direct class coverage
 * separately. This test file ensures the constructors and {@code @ResponseStatus}
 * annotations are exercised at the unit level.</p>
 */
class ExceptionClassesTest {

    @Test
    void adminNotFound_defaultConstructor_carriesMessage() {
        AdminNotFoundException ex = new AdminNotFoundException();
        assertThat(ex.getMessage()).contains("admin user could not be resolved");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void adminNotFound_stringConstructor_overridesMessage() {
        AdminNotFoundException ex = new AdminNotFoundException("custom");
        assertThat(ex.getMessage()).isEqualTo("custom");
    }

    @Test
    void invitationNotFound_uuidConstructor_includesId() {
        java.util.UUID id = java.util.UUID.randomUUID();
        InvitationNotFoundException ex = new InvitationNotFoundException(id);
        assertThat(ex.getMessage()).contains("Invitation not found");
        assertThat(ex.getMessage()).contains(id.toString());
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void invitationNotFound_stringConstructor_carriesMessage() {
        InvitationNotFoundException ex = new InvitationNotFoundException("custom");
        assertThat(ex.getMessage()).isEqualTo("custom");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void lastAdmin_stringConstructor_carriesMessage() {
        LastAdminException ex = new LastAdminException("Cannot remove the only admin");
        assertThat(ex.getMessage()).isEqualTo("Cannot remove the only admin");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
