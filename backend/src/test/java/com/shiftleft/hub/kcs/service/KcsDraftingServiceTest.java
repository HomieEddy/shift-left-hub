package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.ai.service.AiConfigService;
import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.kcs.api.dto.KcsDraftResponse;
import com.shiftleft.hub.kcs.domain.TicketResolvedEvent;
import com.shiftleft.hub.tag.domain.Tag;
import com.shiftleft.hub.tag.domain.TagRepository;
import com.shiftleft.hub.ticket.domain.Ticket;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketRepository;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.ticket.domain.TicketStatus;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KcsDraftingServiceTest {

    @Mock private AiConfigService aiConfigService;
    @Mock private ArticleRepository articleRepository;
    @Mock private TagRepository tagRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private VectorStore vectorStore;
    @Mock private UserRepository userRepository;

    @InjectMocks private KcsDraftingService kcsDraftingService;

    private final UUID ticketId = UUID.randomUUID();
    private final String ticketNumber = "TKT-0005";
    private final UUID articleId = UUID.randomUUID();
    private final UUID systemUserId = UUID.randomUUID();

    private User createSystemUser() {
        return User.builder()
            .id(systemUserId).email("system@shiftleft.local").password("n/a")
            .displayName("KCS System").role(UserRole.ROLE_ADMIN).enabled(true).build();
    }

    private TicketResolvedEvent createEvent() {
        return new TicketResolvedEvent(
            ticketId, ticketNumber,
            "User cannot connect to VPN",
            "{\"chat\":\"...\"}",
            TicketCategory.NETWORK, TicketUrgency.HIGH,
            "Reset VPN credentials and reconfigured client",
            "John Doe", "john@example.com",
            "Agent Smith", LocalDateTime.now()
        );
    }

    private Article createArticle() {
        return Article.builder()
            .id(articleId)
            .titleEn("VPN Connection Guide")
            .contentEn("## Overview\nVPN connection steps...\n## Steps to Resolve\n1. Check credentials\n2. Restart client")
            .titleFr("Guide de connexion VPN")
            .contentFr("## Aperçu\nÉtapes de connexion VPN...")
            .slug("vpn-connection-guide")
            .excerpt("Guide to resolving VPN connection issues")
            .status(ArticleStatus.DRAFT)
            .viewCount(0)
            .author(createSystemUser())
            .sourceTicketId(ticketId)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private Ticket createTicket() {
        User user = User.builder()
            .id(UUID.randomUUID()).email("user@example.com").password("pwd")
            .displayName("Test User").role(UserRole.ROLE_USER).enabled(true).build();
        return Ticket.builder()
            .id(ticketId).ticketNumber(ticketNumber).user(user)
            .status(TicketStatus.RESOLVED).category(TicketCategory.NETWORK)
            .urgency(TicketUrgency.HIGH).issue("Cannot connect to VPN")
            .resolutionNotes("Reset VPN credentials and reconfigured client")
            .createdAt(LocalDateTime.now()).build();
    }

    // Helper to mock ChatClient chain using deep stubs (Spring AI 2.x API)
    private ChatClient mockChatClient(String response) {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn(response);
        return chatClient;
    }

    // ── draftArticle: success ─────────────────────────────────

    @Test
    void draftArticle_shouldSucceed() {
        User systemUser = createSystemUser();
        TicketResolvedEvent event = createEvent();

        String llmResponse = """
            title_en: VPN Connection Guide
            title_fr: Guide de connexion VPN
            excerpt: Guide to resolving VPN connection issues
            content_en:
            ## Overview
            VPN connection steps...
            ## Steps to Resolve
            1. Check credentials
            2. Restart client
            content_fr:
            ## Aperçu
            Étapes de connexion VPN...
            suggested_tags: network, vpn, connectivity
            """;

        AiConfig aiConfig = AiConfig.builder()
            .llmProvider("OLLAMA").ollamaEndpointUrl("http://localhost:11434")
            .chatModelName("llama3.2:3b").embeddingModelName("nomic-embed-text")
            .similarityThreshold(0.65).embeddingDimension(768).build();
        ChatClient chatClient = mockChatClient(llmResponse);

        when(articleRepository.findBySourceTicketId(ticketId)).thenReturn(Optional.empty());
        when(aiConfigService.getConfigEntity()).thenReturn(aiConfig);
        when(aiConfigService.buildChatClient(aiConfig)).thenReturn(chatClient);
        when(articleRepository.findBySlug("vpn-connection-guide")).thenReturn(Optional.empty());
        Tag networkTag = Tag.builder().id(UUID.randomUUID()).nameEn("network").nameFr("réseau").color("#3498db").build();
        when(tagRepository.findByNameEnIn(anyList())).thenReturn(List.of(networkTag));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            return Article.builder()
                .id(articleId).titleEn(a.getTitleEn()).contentEn(a.getContentEn())
                .titleFr(a.getTitleFr()).contentFr(a.getContentFr())
                .slug(a.getSlug()).excerpt(a.getExcerpt())
                .status(ArticleStatus.DRAFT).viewCount(0).author(a.getAuthor())
                .tags(a.getTags()).sourceTicketId(ticketId)
                .createdAt(LocalDateTime.now()).build();
        });
        // checkDuplicates: FTS empty, vector empty
        when(articleRepository.searchByText(anyString(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        Article result = kcsDraftingService.draftArticle(event, systemUser);

        assertNotNull(result);
        assertEquals("VPN Connection Guide", result.getTitleEn());
        assertEquals(ticketId, result.getSourceTicketId());
        verify(articleRepository).save(any(Article.class));
    }

    // ── draftArticle: duplicate skip ──────────────────────────

    @Test
    void draftArticle_shouldReturnExistingWhenDuplicate() {
        User systemUser = createSystemUser();
        TicketResolvedEvent event = createEvent();
        Article existing = createArticle();

        when(articleRepository.findBySourceTicketId(ticketId)).thenReturn(Optional.of(existing));

        Article result = kcsDraftingService.draftArticle(event, systemUser);

        assertSame(existing, result);
        verify(articleRepository, never()).save(any());
        verifyNoInteractions(aiConfigService);
    }

    // ── draftArticle: LLM failure → fallback ──────────────────

    @Test
    void draftArticle_shouldFallbackWhenLlmReturnsMalformedResponse() {
        User systemUser = createSystemUser();
        TicketResolvedEvent event = createEvent();

        // LLM returns blank response (no fields parseable → fallback to issue/resolution)
        AiConfig aiConfig = AiConfig.builder()
            .llmProvider("OLLAMA").ollamaEndpointUrl("http://localhost:11434")
            .chatModelName("llama3.2:3b").embeddingModelName("nomic-embed-text")
            .similarityThreshold(0.65).embeddingDimension(768).build();
        ChatClient chatClient = mockChatClient("");

        when(articleRepository.findBySourceTicketId(ticketId)).thenReturn(Optional.empty());
        when(aiConfigService.getConfigEntity()).thenReturn(aiConfig);
        when(aiConfigService.buildChatClient(aiConfig)).thenReturn(chatClient);
        // slug from event.issue() = "user-cannot-connect-to-vpn"
        when(articleRepository.findBySlug("user-cannot-connect-to-vpn")).thenReturn(Optional.empty());
        // suggestedTags will be empty from blank LLM response — resolveSuggestedTags returns early, no tagRepository call
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            return Article.builder()
                .id(articleId).titleEn(a.getTitleEn()).contentEn(a.getContentEn())
                .slug(a.getSlug()).status(ArticleStatus.DRAFT).viewCount(0)
                .author(a.getAuthor()).sourceTicketId(ticketId)
                .createdAt(LocalDateTime.now()).build();
        });
        when(articleRepository.searchByText(anyString(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        Article result = kcsDraftingService.draftArticle(event, systemUser);

        // Falls back to event.issue() as title, event.resolutionNotes() as content
        assertNotNull(result);
        assertEquals(event.issue(), result.getTitleEn());
        assertTrue(result.getContentEn() != null && result.getContentEn().contains(event.resolutionNotes()));
    }

    // ── draftArticle: slug collision ──────────────────────────

    @Test
    void draftArticle_shouldHandleSlugCollision() {
        User systemUser = createSystemUser();
        TicketResolvedEvent event = createEvent();

        String llmResponse = """
            title_en: VPN Connection Guide
            title_fr: Guide de connexion VPN
            excerpt: Guide to resolving VPN connection issues
            content_en:
            ## Overview
            VPN connection steps...
            content_fr:
            ## Aperçu
            Étapes de connexion VPN...
            suggested_tags: network
            """;

        AiConfig aiConfig = AiConfig.builder()
            .llmProvider("OLLAMA").ollamaEndpointUrl("http://localhost:11434")
            .chatModelName("llama3.2:3b").embeddingModelName("nomic-embed-text")
            .similarityThreshold(0.65).embeddingDimension(768).build();
        ChatClient chatClient = mockChatClient(llmResponse);

        when(articleRepository.findBySourceTicketId(ticketId)).thenReturn(Optional.empty());
        when(aiConfigService.getConfigEntity()).thenReturn(aiConfig);
        when(aiConfigService.buildChatClient(aiConfig)).thenReturn(chatClient);
        // First slug lookup returns existing article → collision
        Article existingArticle = createArticle();
        when(articleRepository.findBySlug("vpn-connection-guide")).thenReturn(Optional.of(existingArticle));
        when(tagRepository.findByNameEnIn(anyList())).thenReturn(List.of());
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            assertTrue(a.getSlug().startsWith("vpn-connection-guide-"),
                "Slug should have UUID suffix: " + a.getSlug());
            return a;
        });
        when(articleRepository.searchByText(anyString(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        kcsDraftingService.draftArticle(event, systemUser);

        verify(articleRepository).save(any(Article.class));
    }

    // ── enrichDraftResponse: with source ticket ───────────────

    @Test
    void enrichDraftResponse_shouldIncludeTicketNumber() {
        Article article = createArticle();
        Ticket ticket = createTicket();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        // findSimilarArticles returns empty
        when(articleRepository.searchByText(anyString(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        KcsDraftResponse response = kcsDraftingService.enrichDraftResponse(article);

        assertNotNull(response);
        assertEquals(ticketNumber, response.sourceTicketNumber());
        assertTrue(response.similarityWarnings().isEmpty());
    }

    // ── enrichDraftResponse: without source ticket ────────────

    @Test
    void enrichDraftResponse_shouldReturnNullTicketNumberWhenNoSource() {
        Article article = createArticle();
        article.setSourceTicketId(null);
        // findSimilarArticles — title is "VPN Connection Guide"
        // Keywords: "vpn connection guide"
        when(articleRepository.searchByText(anyString(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        KcsDraftResponse response = kcsDraftingService.enrichDraftResponse(article);

        assertNull(response.sourceTicketNumber());
    }

    // ── enrichDraftResponse: with similarity warnings ─────────

    @Test
    void enrichDraftResponse_shouldIncludeSimilarityWarnings() {
        Article article = createArticle();
        Ticket ticket = createTicket();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        // FTS returns similar articles
        UUID otherId = UUID.randomUUID();
        Object[] row1 = new Object[]{otherId, "Similar VPN Guide"};
        Object[] row2 = new Object[]{UUID.randomUUID(), "Another VPN Article"};
        Page<Object[]> ftsPage = new PageImpl<>(List.of(row1, row2));
        when(articleRepository.searchByText(anyString(), any(Pageable.class)))
            .thenReturn(ftsPage);

        KcsDraftResponse response = kcsDraftingService.enrichDraftResponse(article);

        assertNotNull(response);
        assertFalse(response.similarityWarnings().isEmpty());
        assertTrue(response.similarityWarnings().contains("Similar VPN Guide"));
    }

    // ── checkDuplicates: FTS + vector results combined ────────

    @Test
    void enrichDraftResponse_shouldHandleDedupFailure() {
        Article article = createArticle();
        article.setTitleEn("");  // empty title — skip dedup
        article.setSourceTicketId(null);

        KcsDraftResponse response = kcsDraftingService.enrichDraftResponse(article);

        assertNotNull(response);
        assertTrue(response.similarityWarnings().isEmpty());
        verifyNoInteractions(ticketRepository);
    }
}
