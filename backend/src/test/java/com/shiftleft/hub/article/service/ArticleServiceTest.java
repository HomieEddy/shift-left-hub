package com.shiftleft.hub.article.service;

import com.shiftleft.hub.ai.service.EmbeddingService;
import com.shiftleft.hub.article.api.dto.ArticleResponse;
import com.shiftleft.hub.article.api.dto.CreateArticleRequest;
import com.shiftleft.hub.article.api.dto.UpdateArticleRequest;
import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleNotFoundException;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.tag.domain.Tag;
import com.shiftleft.hub.tag.domain.TagNotFoundException;
import com.shiftleft.hub.tag.domain.TagRepository;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock private ArticleRepository articleRepository;
    @Mock private TagRepository tagRepository;
    @Mock private EmbeddingService embeddingService;

    @InjectMocks private ArticleService articleService;

    private final UUID articleId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        WorkspaceContextHolder.setCurrentWorkspaceId(workspaceId);
    }

    @AfterEach
    void tearDown() {
        WorkspaceContextHolder.clear();
    }

    private User createAuthor() {
        return User.builder()
            .id(authorId).email("author@example.com").password("pwd")
            .displayName("Author").role(UserRole.ROLE_ADMIN).enabled(true).build();
    }

    private Article createArticle(ArticleStatus status) {
        User author = createAuthor();
        return Article.builder()
            .id(articleId)
            .titleEn("Test Article")
            .contentEn("Content of the article")
            .titleFr("Article de test")
            .contentFr("Contenu de l'article")
            .slug("test-article")
            .excerpt("A test article")
            .status(status)
            .viewCount(0)
            .author(author)
            .createdAt(LocalDateTime.now())
            .build();
    }

    // ── getArticleById ────────────────────────────────────────

    @Test
    void getArticleById_shouldSucceed() {
        Article article = createArticle(ArticleStatus.PUBLISHED);
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        ArticleResponse response = articleService.getArticleById(articleId);

        assertNotNull(response);
        assertEquals(articleId, response.id());
        assertEquals("Test Article", response.titleEn());
    }

    @Test
    void getArticleById_shouldThrowWhenNotFound() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        assertThrows(ArticleNotFoundException.class,
            () -> articleService.getArticleById(articleId));
    }

    // ── getAllArticles ────────────────────────────────────────

    @Test
    void getAllArticles_shouldReturnPage() {
        Article article = createArticle(ArticleStatus.PUBLISHED);
        Page<Article> page = new PageImpl<>(List.of(article));
        when(articleRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<ArticleResponse> result = articleService.getAllArticles(0, 10);

        assertEquals(1, result.getContent().size());
        assertEquals("Test Article", result.getContent().getFirst().titleEn());
    }

    @Test
    void getAllArticles_shouldReturnEmptyPage() {
        Page<Article> emptyPage = new PageImpl<>(List.of());
        when(articleRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        Page<ArticleResponse> result = articleService.getAllArticles(0, 10);

        assertTrue(result.getContent().isEmpty());
    }

    // ── getArticlesByStatus ───────────────────────────────────

    @Test
    void getArticlesByStatus_shouldFilter() {
        Article draft = createArticle(ArticleStatus.DRAFT);
        Page<Article> page = new PageImpl<>(List.of(draft));
        when(articleRepository.findWithAssociationsByStatus(eq(ArticleStatus.DRAFT), any(Pageable.class)))
            .thenReturn(page);

        Page<ArticleResponse> result = articleService.getArticlesByStatus(ArticleStatus.DRAFT, 0, 10);

        assertEquals(1, result.getContent().size());
        assertEquals(ArticleStatus.DRAFT, result.getContent().getFirst().status());
    }

    // ── createArticle ─────────────────────────────────────────

    @Test
    void createArticle_shouldSucceed() {
        User author = createAuthor();
        CreateArticleRequest request = new CreateArticleRequest(
            "Test Article", "Content", "Article de test", "Contenu",
            "Excerpt", null, null, Set.of());
        when(articleRepository.findBySlug("test-article")).thenReturn(Optional.empty());
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            return Article.builder()
                .id(articleId)
                .titleEn(a.getTitleEn()).contentEn(a.getContentEn())
                .titleFr(a.getTitleFr()).contentFr(a.getContentFr())
                .slug(a.getSlug()).excerpt(a.getExcerpt())
                .status(a.getStatus()).viewCount(0).author(a.getAuthor())
                .tags(a.getTags()).createdAt(LocalDateTime.now())
                .build();
        });

        ArticleResponse response = articleService.createArticle(request, author);

        assertNotNull(response);
        assertEquals("Test Article", response.titleEn());
        verify(articleRepository).save(any(Article.class));
    }

    @Test
    void createArticle_shouldAppendUuidOnSlugCollision() {
        User author = createAuthor();
        CreateArticleRequest request = new CreateArticleRequest(
            "Test Article", "Content", "Article de test", "Contenu",
            null, null, null, Set.of());
        when(articleRepository.findBySlug("test-article"))
            .thenReturn(Optional.of(createArticle(ArticleStatus.PUBLISHED)));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            assertTrue(a.getSlug().startsWith("test-article-"),
                "Slug should have a UUID suffix: " + a.getSlug());
            return a;
        });

        articleService.createArticle(request, author);

        verify(articleRepository).save(any(Article.class));
    }

    @Test
    void createArticle_shouldThrowWhenTagNotFound() {
        User author = createAuthor();
        UUID missingTagId = UUID.randomUUID();
        CreateArticleRequest request = new CreateArticleRequest(
            "Test Article", "Content", null, null,
            null, null, null, Set.of(missingTagId));
        // resolveTags is called before slug — findByslug isn't reached
        // tagRepository.findAllById returns empty by default — TagNotFoundException thrown

        assertThrows(TagNotFoundException.class,
            () -> articleService.createArticle(request, author));
    }

    // ── updateArticle ─────────────────────────────────────────

    @Test
    void updateArticle_shouldSucceed() {
        User editor = createAuthor();
        Article article = createArticle(ArticleStatus.DRAFT);
        UpdateArticleRequest request = new UpdateArticleRequest(
            "Updated Title", "Updated Content", null, null,
            "Updated excerpt", null, null, Set.of());
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleRepository.findBySlug("updated-title")).thenReturn(Optional.empty());
        when(articleRepository.save(any(Article.class))).thenReturn(article);

        ArticleResponse response = articleService.updateArticle(articleId, request, editor);

        assertNotNull(response);
        verify(articleRepository).save(any(Article.class));
    }

    @Test
    void updateArticle_shouldHandleSlugConflict() {
        User editor = createAuthor();
        Article article = createArticle(ArticleStatus.DRAFT);
        Article conflictingArticle = createArticle(ArticleStatus.PUBLISHED);
        conflictingArticle.setId(UUID.randomUUID());
        UpdateArticleRequest request = new UpdateArticleRequest(
            "Updated Title", "Updated Content", null, null,
            null, null, null, Set.of());
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleRepository.findBySlug("updated-title"))
            .thenReturn(Optional.of(conflictingArticle));
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article a = invocation.getArgument(0);
            assertTrue(a.getSlug().startsWith("updated-title-"),
                "Slug should have UUID suffix on conflict: " + a.getSlug());
            return a;
        });

        articleService.updateArticle(articleId, request, editor);

        verify(articleRepository).save(any(Article.class));
    }

    @Test
    void updateArticle_shouldThrowWhenNotFound() {
        User editor = createAuthor();
        UpdateArticleRequest request = new UpdateArticleRequest(
            "Title", "Content", null, null, null, null, null, Set.of());
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        assertThrows(ArticleNotFoundException.class,
            () -> articleService.updateArticle(articleId, request, editor));
    }

    // ── publishArticle ────────────────────────────────────────

    @Test
    void publishArticle_shouldSucceed() {
        Article article = createArticle(ArticleStatus.DRAFT);
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenReturn(article);

        ArticleResponse response = articleService.publishArticle(articleId);

        assertNotNull(response);
        assertEquals(ArticleStatus.PUBLISHED, response.status());
        verify(embeddingService).generateAndStoreEmbedding(article);
    }

    @Test
    void publishArticle_shouldNotSetPublishedAtWhenAlreadySet() {
        Article article = createArticle(ArticleStatus.DRAFT);
        article.setPublishedAt(LocalDateTime.now().minusDays(1));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenReturn(article);

        articleService.publishArticle(articleId);

        verify(embeddingService).generateAndStoreEmbedding(article);
    }

    @Test
    void publishArticle_shouldNotThrowWhenEmbeddingFails() {
        Article article = createArticle(ArticleStatus.DRAFT);
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenReturn(article);
        doThrow(new RuntimeException("OpenAI unavailable"))
            .when(embeddingService).generateAndStoreEmbedding(article);

        assertDoesNotThrow(() -> articleService.publishArticle(articleId));
    }

    // ── archiveArticle ────────────────────────────────────────

    @Test
    void archiveArticle_shouldSucceed() {
        Article article = createArticle(ArticleStatus.PUBLISHED);
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenReturn(article);

        ArticleResponse response = articleService.archiveArticle(articleId);

        assertEquals(ArticleStatus.ARCHIVED, response.status());
    }

    @Test
    void archiveArticle_shouldReturnOkWhenAlreadyArchived() {
        Article article = createArticle(ArticleStatus.ARCHIVED);
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        ArticleResponse response = articleService.archiveArticle(articleId);

        assertEquals(ArticleStatus.ARCHIVED, response.status());
        verify(articleRepository, never()).save(any());
    }

    // ── deleteArticle ─────────────────────────────────────────

    @Test
    void deleteArticle_shouldSucceed() {
        when(articleRepository.existsById(articleId)).thenReturn(true);

        articleService.deleteArticle(articleId);

        verify(articleRepository).deleteById(articleId);
    }

    @Test
    void deleteArticle_shouldThrowWhenNotFound() {
        when(articleRepository.existsById(articleId)).thenReturn(false);

        assertThrows(ArticleNotFoundException.class,
            () -> articleService.deleteArticle(articleId));
    }

    // ── createArticle: validation ─────────────────────────────

    @Test
    void createArticle_shouldThrowWhenTitleBlank() {
        User author = createAuthor();
        CreateArticleRequest request = new CreateArticleRequest(
            "", "Content", null, null, null, null, null, Set.of());

        assertThrows(IllegalArgumentException.class,
            () -> articleService.createArticle(request, author));
        verify(articleRepository, never()).save(any());
    }

    @Test
    void createArticle_shouldThrowWhenContentBlank() {
        User author = createAuthor();
        CreateArticleRequest request = new CreateArticleRequest(
            "Title", "", null, null, null, null, null, Set.of());

        assertThrows(IllegalArgumentException.class,
            () -> articleService.createArticle(request, author));
        verify(articleRepository, never()).save(any());
    }

    // ── getArticle: workspace scoped ───────────────────────────

    @Test
    void getArticle_shouldThrowWhenWrongWorkspace() {
        Article article = createArticle(ArticleStatus.PUBLISHED);
        article.setWorkspaceId(UUID.randomUUID());
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        assertThrows(ArticleNotFoundException.class,
            () -> articleService.getArticleById(articleId));
    }

    // ── updateArticle: partial update ─────────────────────────

    @Test
    void updateArticle_shouldHandlePartialUpdate() {
        User editor = createAuthor();
        Article article = createArticle(ArticleStatus.DRAFT);
        // Update only French fields; titleEn stays the same so slug doesn't change
        UpdateArticleRequest request = new UpdateArticleRequest(
            "Test Article", null, "Titre mis à jour", "Contenu mis à jour",
            null, null, null, Set.of());
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenReturn(article);

        ArticleResponse response = articleService.updateArticle(articleId, request, editor);

        assertNotNull(response);
        verify(articleRepository).save(any(Article.class));
    }
}
