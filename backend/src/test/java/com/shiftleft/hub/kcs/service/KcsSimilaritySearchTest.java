package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KcsSimilaritySearchTest {

    @Mock private ArticleRepository articleRepository;

    private KcsSimilaritySearch search;

    private static final UUID WORKSPACE_ID = UUID.randomUUID();
    private static final UUID ARTICLE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        WorkspaceContextHolder.setCurrentWorkspaceId(WORKSPACE_ID);
        search = new KcsSimilaritySearch(articleRepository);
    }

    @AfterEach
    void tearDown() {
        WorkspaceContextHolder.clear();
    }

    private Article articleWithTitle(String title) {
        return Article.builder()
            .id(ARTICLE_ID)
            .titleEn(title)
            .build();
    }

    @Test
    void findSimilarArticles_returnsUpTo3SimilarTitles() {
        Article article = articleWithTitle("VPN Connection Guide");
        UUID otherId = UUID.randomUUID();
        Page<Object[]> page = new PageImpl<>(List.<Object[]>of(
            new Object[]{otherId, "VPN Setup Guide"},
            new Object[]{UUID.randomUUID(), "Network Troubleshooting"},
            new Object[]{UUID.randomUUID(), "VPN Errors"},
            new Object[]{UUID.randomUUID(), "Should be excluded at limit"}
        ));
        when(articleRepository.searchByText(any(), eq(WORKSPACE_ID), any(Pageable.class)))
            .thenReturn(page);

        Set<String> similar = search.findSimilarArticles(article);

        assertEquals(3, similar.size());
        assertTrue(similar.contains("VPN Setup Guide"));
        assertFalse(similar.contains("Should be excluded at limit"));
    }

    @Test
    void findSimilarArticles_excludesTheInputArticleItself() {
        Article article = articleWithTitle("VPN Connection Guide");
        Page<Object[]> page = new PageImpl<>(List.<Object[]>of(
            new Object[]{ARTICLE_ID, "VPN Connection Guide"}, // same id → exclude
            new Object[]{UUID.randomUUID(), "Other VPN Article"}
        ));
        when(articleRepository.searchByText(any(), eq(WORKSPACE_ID), any(Pageable.class)))
            .thenReturn(page);

        Set<String> similar = search.findSimilarArticles(article);

        assertEquals(1, similar.size());
        assertTrue(similar.contains("Other VPN Article"));
    }

    @Test
    void findSimilarArticles_returnsEmptyWhenTitleIsBlank() {
        Article article = articleWithTitle("   ");

        Set<String> similar = search.findSimilarArticles(article);

        assertTrue(similar.isEmpty());
    }

    @Test
    void findSimilarArticles_returnsEmptyWhenTitleIsNull() {
        Article article = articleWithTitle(null);

        Set<String> similar = search.findSimilarArticles(article);

        assertTrue(similar.isEmpty());
    }

    @Test
    void findSimilarArticles_swallowsRepositoryExceptions() {
        Article article = articleWithTitle("VPN Connection Guide");
        when(articleRepository.searchByText(any(), eq(WORKSPACE_ID), any(Pageable.class)))
            .thenThrow(new RuntimeException("db down"));

        Set<String> similar = search.findSimilarArticles(article);

        assertTrue(similar.isEmpty());
    }

    @Test
    void findSimilarArticles_skipsNullTitlesInResults() {
        Article article = articleWithTitle("VPN Connection Guide");
        Page<Object[]> page = new PageImpl<>(List.<Object[]>of(
            new Object[]{UUID.randomUUID(), null},
            new Object[]{UUID.randomUUID(), "Real Title"}
        ));
        when(articleRepository.searchByText(any(), eq(WORKSPACE_ID), any(Pageable.class)))
            .thenReturn(page);

        Set<String> similar = search.findSimilarArticles(article);

        assertEquals(1, similar.size());
        assertTrue(similar.contains("Real Title"));
    }
}
