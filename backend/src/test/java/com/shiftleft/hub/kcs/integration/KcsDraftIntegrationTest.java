package com.shiftleft.hub.kcs.integration;

import com.shiftleft.hub.AbstractIntegrationTest;
import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.kcs.api.dto.KcsDraftResponse;
import com.shiftleft.hub.kcs.service.KcsDraftingService;
import com.shiftleft.hub.ticket.api.dto.CreateTicketRequest;
import com.shiftleft.hub.ticket.domain.TicketCategory;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for the KCS draft pipeline data layer.
 * <p>Verifies that:
 * <ul>
 *   <li>An Article entity can be saved with a {@code sourceTicketId} link</li>
 *   <li>DRAFT status and timeline fields are set correctly</li>
 *   <li>The article can be found via {@link ArticleRepository#findBySourceTicketId(UUID)}</li>
 *   <li>The unique constraint on {@code source_ticket_id} prevents duplicates</li>
 *   <li>{@link KcsDraftingService#enrichDraftResponse(Article)} works end-to-end without LLM calls</li>
 * </ul>
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KcsDraftIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private KcsDraftingService kcsDraftingService;

    private WebTestClient webTestClient;
    private String userAccessToken;
    private UUID ticketId;
    private User adminUser;
    private Article savedDraft;

    private static final String USER_EMAIL = "kcs-user@shiftleft.local";
    private static final String ADMIN_EMAIL = "kcs-admin@shiftleft.local";
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
    void setUp() {
        // Create regular user for ticket creation
        var registerRequest = new RegisterRequest(USER_EMAIL, PASSWORD, "KCS User");
        AuthResponse userReg = client().post().uri("/api/auth/register")
                .bodyValue(registerRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();
        userAccessToken = userReg.accessToken();

        // Create admin user for article author
        adminUser = userRepository.findByEmail(ADMIN_EMAIL).orElseGet(() ->
            userRepository.save(User.builder()
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(PASSWORD))
                .displayName("KCS Admin")
                .role(UserRole.ROLE_ADMIN)
                .enabled(true)
                .build())
        );

        // Create a ticket to serve as the source ticket
        var ticketRequest = new CreateTicketRequest(
                "Email not syncing on mobile device after Exchange update",
                TicketCategory.SOFTWARE,
                TicketUrgency.MEDIUM,
                "{\"chatTranscript\":\"User cannot receive emails on iPhone since IT pushed Exchange update\"}");

        com.shiftleft.hub.ticket.api.dto.TicketResponse ticketResp = client().post()
                .uri("/api/tickets")
                .cookie("access_token", userAccessToken)
                .bodyValue(ticketRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(com.shiftleft.hub.ticket.api.dto.TicketResponse.class)
                .returnResult().getResponseBody();

        ticketId = ticketResp.id();
    }

    @Test
    void t01_saveDraftArticleWithSourceTicketLink_shouldSucceed() {
        savedDraft = Article.builder()
                .titleEn("Email Sync Issues After Exchange Update")
                .contentEn("## Overview\nUsers may experience email sync issues on mobile devices " +
                        "after an Exchange server update.\n\n## Steps to Resolve\n" +
                        "1. Remove the email account from the device\n" +
                        "2. Re-add the account with the updated server settings\n" +
                        "3. Verify sync is working\n\n## Notes\nEnsure the device is running " +
                        "the latest OS version before reconfiguring.")
                .titleFr("Problèmes de synchronisation des e-mails après la mise à jour Exchange")
                .contentFr("## Aperçu\nLes utilisateurs peuvent rencontrer des problèmes de " +
                        "synchronisation des e-mails...")
                .slug("email-sync-issues-after-exchange-update")
                .excerpt("Steps to resolve email sync issues on mobile devices after Exchange update.")
                .status(ArticleStatus.DRAFT)
                .viewCount(0)
                .author(adminUser)
                .sourceTicketId(ticketId)
                .build();

        savedDraft = articleRepository.save(savedDraft);

        assertThat(savedDraft).isNotNull();
        assertThat(savedDraft.getId()).isNotNull();
        assertThat(savedDraft.getSourceTicketId()).isEqualTo(ticketId);
        assertThat(savedDraft.getCreatedAt()).isNotNull();
    }

    @Test
    void t02_draftArticleShouldHaveCorrectStatusAndContent() {
        Article draft = articleRepository.findById(savedDraft.getId())
                .orElseThrow(() -> new AssertionError("Draft article not found"));

        assertThat(draft.getStatus()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(draft.getTitleEn()).isEqualTo("Email Sync Issues After Exchange Update");
        assertThat(draft.getContentEn()).isNotBlank();
        assertThat(draft.getSlug()).isNotBlank();
        assertThat(draft.getCreatedAt()).isNotNull();
        assertThat(draft.getUpdatedAt()).isNotNull();
        assertThat(draft.getAuthor().getId()).isEqualTo(adminUser.getId());
    }

    @Test
    void t03_findBySourceTicketId_shouldReturnDraft() {
        Optional<Article> found = articleRepository.findBySourceTicketId(ticketId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(savedDraft.getId());
        assertThat(found.get().getTitleEn()).contains("Email Sync");
    }

    @Test
    void t04_duplicateSourceTicketId_shouldBeRejectedByUniqueConstraint() {
        Article duplicate = Article.builder()
                .titleEn("Duplicate Draft")
                .contentEn("This should fail due to unique constraint on source_ticket_id.")
                .slug("duplicate-draft")
                .excerpt("Duplicate test.")
                .status(ArticleStatus.DRAFT)
                .viewCount(0)
                .author(adminUser)
                .sourceTicketId(ticketId)  // Same sourceTicketId
                .build();

        assertThatThrownBy(() -> articleRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void t05_enrichDraftResponse_shouldIncludeSourceTicketNumber() {
        KcsDraftResponse enriched = kcsDraftingService.enrichDraftResponse(savedDraft);

        assertThat(enriched).isNotNull();
        assertThat(enriched.id()).isEqualTo(savedDraft.getId());
        assertThat(enriched.titleEn()).isEqualTo("Email Sync Issues After Exchange Update");
        assertThat(enriched.status()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(enriched.sourceTicketId()).isEqualTo(ticketId);
        assertThat(enriched.sourceTicketNumber()).isNotNull();
        assertThat(enriched.sourceTicketNumber()).startsWith("TKT-");
        assertThat(enriched.similarityWarnings()).isNotNull();
    }
}
