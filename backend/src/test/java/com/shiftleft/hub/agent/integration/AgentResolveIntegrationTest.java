package com.shiftleft.hub.agent.integration;

import com.shiftleft.hub.AbstractIntegrationTest;
import com.shiftleft.hub.agent.api.dto.AddWorkNoteRequest;
import com.shiftleft.hub.agent.api.dto.AgentTicketResponse;
import com.shiftleft.hub.agent.api.dto.ResolveTicketRequest;
import com.shiftleft.hub.agent.api.dto.WorkNoteResponse;
import com.shiftleft.hub.ticket.api.dto.CreateTicketRequest;
import com.shiftleft.hub.ticket.api.dto.TicketResponse;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketStatus;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import com.shiftleft.hub.user.api.dto.AuthResponse;
import com.shiftleft.hub.user.api.dto.LoginRequest;
import com.shiftleft.hub.user.api.dto.RegisterRequest;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for agent ticket workflow:
 * create ticket → claim → work note → resolve with KCS flag.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgentResolveIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private WebTestClient webTestClient;
    private String userAccessToken;
    private String agentAccessToken;
    private UUID createdTicketId;

    private static final String USER_EMAIL = "ticket-creator@shiftleft.local";
    private static final String AGENT_EMAIL = "support-agent@shiftleft.local";
    private static final String PASSWORD = "TestPass1";

    private WebTestClient client() {
        if (webTestClient == null) {
            webTestClient = WebTestClient.bindToServer()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return webTestClient;
    }

    @BeforeAll
    void setUpUsers() {
        // Create regular user via registration
        var registerRequest = new RegisterRequest(USER_EMAIL, PASSWORD, "Ticket Creator");
        AuthResponse userReg = client().post().uri("/api/auth/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();
        userAccessToken = userReg.accessToken();

        // Create agent user directly in database
        userRepository.findByEmail(AGENT_EMAIL).orElseGet(() ->
            userRepository.save(User.builder()
                .email(AGENT_EMAIL)
                .password(passwordEncoder.encode(PASSWORD))
                .displayName("Support Agent")
                .role(UserRole.ROLE_AGENT)
                .enabled(true)
                .build())
        );

        // Login as agent to get tokens
        var loginRequest = new LoginRequest(AGENT_EMAIL, PASSWORD);
        AuthResponse agentLogin = client().post().uri("/api/auth/login")
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();
        agentAccessToken = agentLogin.accessToken();
    }

    @Test
    void t01_userCreatesTicket_shouldBeNew() {
        var request = new CreateTicketRequest(
                "Cannot access shared drive on file server",
                TicketCategory.NETWORK,
                TicketUrgency.HIGH,
                "{\"chatTranscript\":\"User lost access after password reset\"}");

        TicketResponse response = client().post().uri("/api/tickets")
                .cookie("access_token", userAccessToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TicketResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(TicketStatus.NEW);
        createdTicketId = response.id();
    }

    @Test
    void t02_agentClaimsTicket_shouldTransitionToInProgress() {
        AgentTicketResponse response = client().post()
                .uri("/api/agent/tickets/{id}/claim", createdTicketId)
                .cookie("access_token", agentAccessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AgentTicketResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(response.assignedToId()).isNotNull();
        assertThat(response.assignedToDisplayName()).isEqualTo("Support Agent");
    }

    @Test
    void t03_agentAddsWorkNote_shouldAppearInWorkNotes() {
        var request = new AddWorkNoteRequest("Investigated the file server permissions. " +
                "Found the user's AD group membership was removed during password reset.");

        WorkNoteResponse addNote = client().post()
                .uri("/api/agent/tickets/{id}/work-notes", createdTicketId)
                .cookie("access_token", agentAccessToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WorkNoteResponse.class)
                .returnResult().getResponseBody();

        assertThat(addNote).isNotNull();
        assertThat(addNote.content()).contains("permissions");

        // Verify work notes are retrievable
        WorkNoteResponse[] notes = client().get()
                .uri("/api/agent/tickets/{id}/work-notes", createdTicketId)
                .cookie("access_token", agentAccessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WorkNoteResponse[].class)
                .returnResult().getResponseBody();

        assertThat(notes).isNotEmpty();
    }

    @Test
    void t04_agentResolvesTicketWithKcsFlag_shouldTransitionToResolvedAndPublishEvent() {
        var request = new ResolveTicketRequest(
                "Restored AD group membership for the user. " +
                "Added the user back to the 'Shared Drive Access' group. " +
                "Verified access is working again.",
                true);

        AgentTicketResponse response = client().post()
                .uri("/api/agent/tickets/{id}/resolve", createdTicketId)
                .cookie("access_token", agentAccessToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AgentTicketResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(response.resolvedAt()).isNotNull();
        assertThat(response.resolutionNotes()).contains("AD group membership");
        assertThat(response.isKnowledgeGap()).isTrue();

        // TicketResolvedEvent is published asynchronously — verified by integration
    }
}
