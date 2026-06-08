package com.shiftleft.hub.ticket.integration;

import com.shiftleft.hub.AbstractIntegrationTest;
import com.shiftleft.hub.ticket.api.dto.CreateTicketRequest;
import com.shiftleft.hub.ticket.api.dto.TicketResponse;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketStatus;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import com.shiftleft.hub.user.api.dto.AuthResponse;
import com.shiftleft.hub.user.api.dto.RegisterRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test covering ticket CRUD operations:
 * create → query → get by ID → cancel → error cases → sequential numbering.
 * <p>All tests run against a real pgvector PostgreSQL container.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.MethodName.class)
class TicketCrudIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    private static String accessToken;
    private static UUID createdTicketId;
    private static String createdTicketNumber;
    private static final String EMAIL = "ticket-crud@shiftleft.local";
    private static final String PASSWORD = "TestPass1";
    private static final String DISPLAY_NAME = "Ticket CRUD User";

    private WebTestClient client() {
        if (webTestClient == null) {
            webTestClient = WebTestClient.bindToServer()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return webTestClient;
    }

    @Test
    void t01_registerUser_shouldSucceed() {
        var request = new RegisterRequest(EMAIL, PASSWORD, DISPLAY_NAME);

        AuthResponse response = client().post().uri("/api/auth/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        accessToken = response.accessToken();
    }

    @Test
    void t02_createTicket_shouldReturnCreatedWithTktNumber() {
        var request = new CreateTicketRequest(
                "Cannot connect to VPN from remote office",
                TicketCategory.NETWORK,
                TicketUrgency.HIGH,
                "{\"chatTranscript\":\"User reported VPN issues...\"}");

        TicketResponse response = client().post().uri("/api/tickets")
                .cookie("access_token", accessToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TicketResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.ticketNumber()).startsWith("TKT-");
        assertThat(response.status()).isEqualTo(TicketStatus.NEW);
        assertThat(response.category()).isEqualTo(TicketCategory.NETWORK);
        assertThat(response.urgency()).isEqualTo(TicketUrgency.HIGH);
        assertThat(response.issue()).isEqualTo("Cannot connect to VPN from remote office");
        assertThat(response.shiftLeftContext()).isEqualTo("{\"chatTranscript\":\"User reported VPN issues...\"}");
        assertThat(response.userId()).isNotNull();

        createdTicketId = response.id();
        createdTicketNumber = response.ticketNumber();
    }

    @Test
    void t03_getMyTickets_shouldIncludeCreatedTicket() {
        List<TicketResponse> tickets = client().get().uri("/api/tickets")
                .cookie("access_token", accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<TicketResponse>>() {})
                .returnResult().getResponseBody();

        assertThat(tickets).isNotEmpty();

        TicketResponse match = tickets.stream()
                .filter(t -> t.id().equals(createdTicketId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Created ticket not found in user's ticket list"));
        assertThat(match.ticketNumber()).isEqualTo(createdTicketNumber);
    }

    @Test
    void t04_getTicketById_shouldReturnFullDetails() {
        TicketResponse response = client().get().uri("/api/tickets/{id}", createdTicketId)
                .cookie("access_token", accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TicketResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(createdTicketId);
        assertThat(response.ticketNumber()).isEqualTo(createdTicketNumber);
        assertThat(response.status()).isEqualTo(TicketStatus.NEW);
        assertThat(response.issue()).isEqualTo("Cannot connect to VPN from remote office");
        assertThat(response.userDisplayName()).isEqualTo(DISPLAY_NAME);
    }

    @Test
    void t05_cancelTicket_shouldChangeStatusToCancelled() {
        TicketResponse response = client().post().uri("/api/tickets/{id}/cancel", createdTicketId)
                .cookie("access_token", accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TicketResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(response.cancelledAt()).isNotNull();
    }

    @Test
    void t06_cancelAlreadyCancelledTicket_shouldReturnError() {
        client().post().uri("/api/tickets/{id}/cancel", createdTicketId)
                .cookie("access_token", accessToken)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void t07_getNonExistentTicket_shouldReturn404() {
        client().get().uri("/api/tickets/{id}", UUID.randomUUID())
                .cookie("access_token", accessToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void t08_createSecondTicket_shouldIncrementSequence() {
        var request = new CreateTicketRequest(
                "Printer not working on floor 3",
                TicketCategory.PERIPHERALS,
                TicketUrgency.LOW,
                null);

        TicketResponse response = client().post().uri("/api/tickets")
                .cookie("access_token", accessToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TicketResponse.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.ticketNumber()).startsWith("TKT-");
        assertThat(response.ticketNumber()).isNotEqualTo(createdTicketNumber);
        assertThat(response.status()).isEqualTo(TicketStatus.NEW);
    }
}
