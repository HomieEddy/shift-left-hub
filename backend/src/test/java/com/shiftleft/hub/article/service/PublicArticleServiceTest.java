package com.shiftleft.hub.article.service;

import com.shiftleft.hub.article.api.dto.ArticleResponse;
import com.shiftleft.hub.article.api.dto.ArticleSearchResult;
import com.shiftleft.hub.article.api.dto.ArticleSearchTag;
import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleNotFoundException;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.tag.domain.Tag;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRole;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.domain.WorkspaceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicArticleServiceTest {

    @Mock private ArticleRepository articleRepository;
    @Mock private WorkspaceRepository workspaceRepository;

    @InjectMocks private PublicArticleService publicArticleService;

    private final UUID workspaceId = UUID.randomUUID();
    private final UUID articleId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();

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

    private Article createPublishedArticle() {
        return Article.builder()
            .id(articleId)
            .titleEn("Published Article")
            .contentEn("Content")
            .slug("published-article")
            .excerpt("Excerpt")
            .status(ArticleStatus.PUBLISHED)
            .viewCount(10)
            .publishedAt(LocalDateTime.now())
            .author(createAuthor())
            .tags(Set.of())
            .createdAt(LocalDateTime.now())
            .build();
    }

    private Article createDraftArticle() {
        return Article.builder()
            .id(articleId)
            .titleEn("Draft Article")
            .contentEn("Draft content")
            .slug("draft-article")
            .status(ArticleStatus.DRAFT)
            .author(createAuthor())
            .tags(Set.of())
            .createdAt(LocalDateTime.now())
            .build();
    }

    // ── getPublishedArticles ──────────────────────────────────

    @Test
    void getPublishedArticles_shouldReturnOnlyPublished() {
        Article article = createPublishedArticle();
        Page<Article> page = new PageImpl<>(List.of(article));
        when(articleRepository.findByStatusAndWorkspaceId(
            eq(ArticleStatus.PUBLISHED), eq(workspaceId), any(Pageable.class)))
            .thenReturn(page);

        Page<ArticleResponse> result = publicArticleService.getPublishedArticles(0, 10);

        assertEquals(1, result.getContent().size());
        assertEquals("Published Article", result.getContent().getFirst().titleEn());
    }

    @Test
    void getPublishedArticles_shouldReturnEmptyPageWhenNoMatch() {
        Page<Article> emptyPage = new PageImpl<>(List.of());
        when(articleRepository.findByStatusAndWorkspaceId(
            eq(ArticleStatus.PUBLISHED), eq(workspaceId), any(Pageable.class)))
            .thenReturn(emptyPage);

        Page<ArticleResponse> result = publicArticleService.getPublishedArticles(0, 10);

        assertTrue(result.getContent().isEmpty());
    }

    // ── getPublishedArticleById ───────────────────────────────

    @Test
    void getPublishedArticleById_shouldSucceed() {
        Article article = createPublishedArticle();
        when(articleRepository.findByIdAndWorkspaceId(articleId, workspaceId))
            .thenReturn(Optional.of(article));

        ArticleResponse result = publicArticleService.getPublishedArticleById(articleId);

        assertNotNull(result);
        assertEquals(articleId, result.id());
        assertEquals(ArticleStatus.PUBLISHED, result.status());
    }

    @Test
    void getPublishedArticleById_shouldThrowWhenDraft() {
        Article draft = createDraftArticle();
        when(articleRepository.findByIdAndWorkspaceId(articleId, workspaceId))
            .thenReturn(Optional.of(draft));

        assertThrows(ArticleNotFoundException.class,
            () -> publicArticleService.getPublishedArticleById(articleId));
    }

    @Test
    void getPublishedArticleById_shouldThrowWhenNotFound() {
        UUID missingId = UUID.randomUUID();
        when(articleRepository.findByIdAndWorkspaceId(missingId, workspaceId))
            .thenReturn(Optional.empty());

        assertThrows(ArticleNotFoundException.class,
            () -> publicArticleService.getPublishedArticleById(missingId));
    }

    // ── search ────────────────────────────────────────────────

    @Test
    void searchPublished_shouldReturnOnlyPublishedArticles() {
        UUID id = UUID.randomUUID();
        Object[] row = {id, "Result Title", "Titre résultat", "result-slug",
            "Result excerpt", LocalDateTime.now(), "Highlight EN", "Highlight FR",
            new String[]{"tag1", "tag2"}};
        Page<Object[]> resultPage = new PageImpl<>(List.<Object[]>of(row));
        when(articleRepository.searchByText(eq("test"), eq(workspaceId), any(Pageable.class)))
            .thenReturn(resultPage);

        Page<ArticleSearchResult> results = publicArticleService.search("test", 0, 10, null);

        assertEquals(1, results.getContent().size());
        assertEquals("Result Title", results.getContent().getFirst().title());
        assertEquals(Set.of("tag1", "tag2"), results.getContent().getFirst().tagNames());
    }

    @Test
    void searchPublished_shouldReturnEmptyPageWhenNoMatch() {
        Page<Object[]> emptyPage = new PageImpl<Object[]>(Collections.emptyList());
        when(articleRepository.searchByText(eq("nothing"), eq(workspaceId), any(Pageable.class)))
            .thenReturn(emptyPage);

        Page<ArticleSearchResult> results = publicArticleService.search("nothing", 0, 10, null);

        assertTrue(results.getContent().isEmpty());
    }

    @Test
    void searchPublished_shouldFilterByTags() {
        UUID id = UUID.randomUUID();
        Object[] row = {id, "Tagged Article", "Article tagué", "tagged-slug",
            "Excerpt", LocalDateTime.now(), "Headline EN", "Headline FR",
            new String[]{"hr-tag"}};
        Page<Object[]> resultPage = new PageImpl<>(List.<Object[]>of(row));
        List<String> tags = List.of("hr-tag");
        when(articleRepository.searchByTextAndTagNames(
            eq("test"), eq(tags), eq(workspaceId), any(Pageable.class)))
            .thenReturn(resultPage);

        Page<ArticleSearchResult> results = publicArticleService.search("test", 0, 10, tags);

        assertEquals(1, results.getContent().size());
    }

    // ── getSearchTags ─────────────────────────────────────────

    @Test
    void getSearchTags_shouldReturnFacets() {
        Object[] facetRow = {"IT Support", "Support IT", "#ff0000", 5L};
        when(articleRepository.findPublishedTagFacets(workspaceId))
            .thenReturn(List.<Object[]>of(facetRow));

        List<ArticleSearchTag> tags = publicArticleService.getSearchTags();

        assertEquals(1, tags.size());
        assertEquals("IT Support", tags.getFirst().nameEn());
        assertEquals("Support IT", tags.getFirst().nameFr());
        assertEquals("#ff0000", tags.getFirst().color());
        assertEquals(5, tags.getFirst().articleCount());
    }

    @Test
    void getSearchTags_shouldReturnEmptyWhenNoArticles() {
        when(articleRepository.findPublishedTagFacets(workspaceId))
            .thenReturn(List.of());

        List<ArticleSearchTag> tags = publicArticleService.getSearchTags();

        assertTrue(tags.isEmpty());
    }
}
