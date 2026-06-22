package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock private VectorStore vectorStore;
    @Mock private EmbeddingModel embeddingModel;
    @Mock private EmbeddingModelProvider embeddingProvider;
    @Mock private ArticleRepository articleRepository;
    @Mock private AiConfigService aiConfigService;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        lenient().when(embeddingProvider.getEmbeddingModel()).thenReturn(embeddingModel);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq("public")))
            .thenReturn("vector(768)");
        ReflectionTestUtils.setField(embeddingService, "vectorStoreDimensions", 768);
        ReflectionTestUtils.setField(embeddingService, "vectorStoreSchema", "public");
    }

    private final UUID articleId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();

    private User createAuthor() {
        return User.builder()
            .id(authorId).email("author@example.com").password("pwd")
            .displayName("Author").role(UserRole.ROLE_ADMIN).enabled(true).build();
    }

    private Article createArticle() {
        Article article = Article.builder()
            .id(articleId)
            .titleEn("Test Article")
            .contentEn("Content of the article")
            .slug("test-article")
            .status(ArticleStatus.PUBLISHED)
            .author(createAuthor())
            .build();
        article.setWorkspaceId(UUID.randomUUID());
        return article;
    }

    // ── storeEmbedding ────────────────────────────────────────

    @Test
    void storeEmbedding_shouldAddDocumentToVectorStore() {
        Article article = createArticle();

        embeddingService.storeEmbedding(article);

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.captor();
        verify(vectorStore).add(captor.capture());
        List<Document> docs = captor.getValue();
        assertEquals(1, docs.size());
        assertEquals("Content of the article\n---\n", docs.getFirst().getText());
        assertEquals(articleId.toString(), docs.getFirst().getMetadata().get("articleId"));
        assertEquals(article.getWorkspaceId().toString(), docs.getFirst().getMetadata().get("workspace_id"));
    }

    @Test
    void storeEmbedding_shouldIncludeFrenchContent() {
        Article article = createArticle();
        article.setContentFr("Contenu de l'article");

        embeddingService.storeEmbedding(article);

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.captor();
        verify(vectorStore).add(captor.capture());
        String content = captor.getValue().getFirst().getText();
        assertTrue(content.contains("Content of the article"));
        assertTrue(content.contains("Contenu de l'article"));
    }

    // ── generateAndStoreEmbedding ─────────────────────────────

    @Test
    void generateAndStoreEmbedding_shouldCallStoreEmbedding() {
        Article article = createArticle();

        embeddingService.generateAndStoreEmbedding(article);

        verify(vectorStore).add(anyList());
    }

    @Test
    void generateAndStoreEmbedding_shouldNotThrowOnFailure() {
        Article article = createArticle();
        doThrow(new RuntimeException("Store failed"))
            .when(vectorStore).add(anyList());

        assertDoesNotThrow(() -> embeddingService.generateAndStoreEmbedding(article));
    }

    // ── reEmbedAll ───────────────────────────────────────────

    @Test
    void reEmbedAll_shouldProcessPublishedArticles() {
        Article article = createArticle();
        Page<Article> page = new PageImpl<>(List.of(article));
        when(articleRepository.findByStatus(ArticleStatus.PUBLISHED, Pageable.unpaged()))
            .thenReturn(page);

        embeddingService.reEmbedAll();

        verify(vectorStore, atLeastOnce()).add(anyList());
    }

    @Test
    void reEmbedAll_shouldHandleEmptyList() {
        Page<Article> emptyPage = new PageImpl<>(List.of());
        when(articleRepository.findByStatus(ArticleStatus.PUBLISHED, Pageable.unpaged()))
            .thenReturn(emptyPage);

        embeddingService.reEmbedAll();

        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void reEmbedAll_shouldNotThrowWhenIndividualArticleFails() {
        Article article = createArticle();
        Page<Article> page = new PageImpl<>(List.of(article));
        when(articleRepository.findByStatus(ArticleStatus.PUBLISHED, Pageable.unpaged()))
            .thenReturn(page);
        doThrow(new RuntimeException("Failure")).when(vectorStore).add(anyList());

        assertDoesNotThrow(() -> embeddingService.reEmbedAll());
    }

    @Test
    void reEmbedAll_shouldClearWorkspaceContextBeforeQueryingAndRestoreAfter() {
        UUID adminWorkspaceId = UUID.randomUUID();
        WorkspaceContextHolder.setCurrentWorkspaceId(adminWorkspaceId);
        try {
            Article article = createArticle();
            Page<Article> page = new PageImpl<>(List.of(article));
            when(articleRepository.findByStatus(eq(ArticleStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(page);

            embeddingService.reEmbedAll();

            verify(articleRepository).findByStatus(eq(ArticleStatus.PUBLISHED), any(Pageable.class));
            verify(vectorStore, atLeastOnce()).add(anyList());
            assertEquals(adminWorkspaceId, WorkspaceContextHolder.getCurrentWorkspaceId(),
                "admin's workspace context must be restored after re-embed");
        } finally {
            WorkspaceContextHolder.clear();
        }
    }

    @Test
    void reEmbedAll_shouldNotThrowWhenNoWorkspaceContextIsSet() {
        WorkspaceContextHolder.clear();
        Page<Article> page = new PageImpl<>(List.of());
        when(articleRepository.findByStatus(eq(ArticleStatus.PUBLISHED), any(Pageable.class)))
            .thenReturn(page);

        assertDoesNotThrow(() -> embeddingService.reEmbedAll());
    }
}
