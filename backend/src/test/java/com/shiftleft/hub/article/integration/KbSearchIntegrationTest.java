package com.shiftleft.hub.article.integration;

import com.shiftleft.hub.AbstractIntegrationTest;
import com.shiftleft.hub.article.api.dto.ArticleResponse;
import com.shiftleft.hub.article.api.dto.CreateArticleRequest;
import com.shiftleft.hub.user.api.dto.AuthResponse;
import com.shiftleft.hub.user.api.dto.LoginRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for KB full-text search using real tsvector/GIN indexes.
 * <p>Creates published articles and verifies FTS search returns correct results,
 * non-matching queries return empty, and DRAFT articles are excluded.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KbSearchIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private WebTestClient webTestClient;
    private String adminAccessToken;
    private static final String ADMIN_EMAIL = "kb-admin@shiftleft.local";
    private static final String ADMIN_PASSWORD = "AdminPass1";

    private WebTestClient client() {
        if (webTestClient == null) {
            webTestClient = WebTestClient.bindToServer()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return webTestClient;
    }

    @BeforeAll
    void setUpAdmin() {
        // Create an admin user directly in the database
        userRepository.findByEmail(ADMIN_EMAIL).orElseGet(() ->
            userRepository.save(User.builder()
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .displayName("KB Admin")
                .role(UserRole.ROLE_ADMIN)
                .enabled(true)
                .build())
        );

        // Login to get auth tokens
        var loginRequest = new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD);
        AuthResponse loginResponse = client().post().uri("/api/auth/login")
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();
        adminAccessToken = loginResponse.accessToken();
    }

    @Test
    void t01_createAndPublishThreeArticles_shouldSucceed() {
        UUID a1 = createArticle(
                "VPN Connection Troubleshooting",
                "This article explains how to troubleshoot VPN connection issues including " +
                "checking your network adapter, verifying credentials, and restarting the VPN client. " +
                "Common causes include expired certificates and incorrect server addresses.");
        UUID a2 = createArticle(
                "Printer Installation Guide",
                "Follow these steps to install a network printer on your workstation. " +
                "Make sure the printer is powered on and connected to the same network. " +
                "Install the latest drivers from the manufacturer website.");
        UUID a3 = createArticle(
                "Email Client Configuration",
                "Configure your email client with IMAP and SMTP settings. " +
                "Use the company's mail server address and your network credentials. " +
                "Enable SSL/TLS encryption for secure communication.");

        publishArticle(a1);
        publishArticle(a2);
        publishArticle(a3);
    }

    @Test
    void t02_searchBySingleKeyword_shouldReturnMatchingArticles() {
        String body = client().get().uri("/api/articles/search?q=VPN")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body).contains("\"content\"");
        assertThat(body).contains("<mark>");
    }

    @Test
    void t03_searchByKeywordInMultipleArticles_shouldReturnAllMatches() {
        String body = client().get().uri("/api/articles/search?q=network")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertThat(body).isNotNull();

        // Check totalElements >= 2 using JsonPath
        Integer total = com.jayway.jsonpath.JsonPath.read(body, "$.totalElements");
        assertThat(total).isGreaterThanOrEqualTo(2);
    }

    @Test
    void t04_searchNonMatchingQuery_shouldReturnEmptyResults() {
        String body = client().get().uri("/api/articles/search?q=xyznonexistent12345")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertThat(body).isNotNull();

        Integer total = com.jayway.jsonpath.JsonPath.read(body, "$.totalElements");
        assertThat(total).isZero();
    }

    @Test
    void t05_searchDraftArticle_shouldNotAppearInResults() {
        // Create a DRAFT article (not published) with distinctive content
        UUID draftId = createArticle(
                "Draft Article About Zebras",
                "Zebras are African equines with distinctive black-and-white striped coats. " +
                "This article is still in draft and should not appear in FTS search results.");

        // Verify it was created as DRAFT
        ArticleResponse getResponse = client().get().uri("/api/admin/articles/{id}", draftId)
                .cookie("access_token", adminAccessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ArticleResponse.class)
                .returnResult().getResponseBody();
        assertThat(getResponse.status().name()).isEqualTo("DRAFT");

        // Search for its distinctive content — should NOT find it
        String searchBody = client().get().uri("/api/articles/search?q=Zebras")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        Integer total = com.jayway.jsonpath.JsonPath.read(searchBody, "$.totalElements");
        assertThat(total).isZero();
    }

    private UUID createArticle(String titleEn, String contentEn) {
        var request = new CreateArticleRequest(titleEn, contentEn, null, null, null, null, null);
        ArticleResponse response = client().post().uri("/api/admin/articles")
                .cookie("access_token", adminAccessToken)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ArticleResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        return response.id();
    }

    private void publishArticle(UUID articleId) {
        ArticleResponse response = client().put().uri("/api/admin/articles/{id}/publish", articleId)
                .cookie("access_token", adminAccessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ArticleResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.status().name()).isEqualTo("PUBLISHED");
        // Populate tsvector columns (trigger not available in test profile)
        jdbcTemplate.update("UPDATE article SET tsv_en = to_tsvector('english', coalesce(title_en,'') || ' ' || coalesce(content_en,'')), tsv_fr = to_tsvector('french', coalesce(title_fr,'') || ' ' || coalesce(content_fr,'')) WHERE id = ?", articleId);
    }
}
