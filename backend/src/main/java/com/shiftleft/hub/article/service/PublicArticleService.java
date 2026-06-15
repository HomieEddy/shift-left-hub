package com.shiftleft.hub.article.service;

import com.shiftleft.hub.article.api.dto.ArticleResponse;
import com.shiftleft.hub.article.api.dto.ArticleSearchResult;
import com.shiftleft.hub.article.api.dto.ArticleSearchTag;
import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleNotFoundException;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.workspace.domain.WorkspaceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PublicArticleService {

    private static final String PUBLIC_SLUG = "public";

    private final ArticleRepository articleRepository;
    private final WorkspaceRepository workspaceRepository;

    private UUID publicWorkspaceId;

    @PostConstruct
    void init() {
        try {
            workspaceRepository.findBySlug(PUBLIC_SLUG)
                .ifPresent(ws -> publicWorkspaceId = ws.getId());
        } catch (Exception e) {
            // Railway startup can race schema initialization; fall back to lazy resolution.
            log.warn("Public workspace lookup deferred: {}", e.getMessage());
        }
    }

    private UUID resolveWorkspaceId() {
        if (WorkspaceContextHolder.hasCurrentWorkspaceId()) {
            return WorkspaceContextHolder.getCurrentWorkspaceId();
        }
        return publicWorkspaceId;
    }

    /**
     * Retrieves published articles for the current workspace (authenticated user)
     * or the Public workspace (anonymous).
     *
     * @param page the page index (zero-based)
     * @param size the page size
     * @return a page of article responses scoped to the effective workspace
     */
    public Page<ArticleResponse> getPublishedArticles(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        UUID wsId = resolveWorkspaceId();
        if (wsId != null) {
            return articleRepository.findByStatusAndWorkspaceId(ArticleStatus.PUBLISHED, wsId, pageable)
                .map(ArticleResponse::from);
        }
        return articleRepository.findByStatus(ArticleStatus.PUBLISHED, pageable).map(ArticleResponse::from);
    }

    /**
     * Retrieves a published article by ID, scoped to the effective workspace.
     *
     * @param id the article UUID
     * @return the published article response
     */
    public ArticleResponse getPublishedArticleById(UUID id) {
        UUID wsId = resolveWorkspaceId();
        Optional<Article> article = wsId != null
            ? articleRepository.findByIdAndWorkspaceId(id, wsId)
            : articleRepository.findById(id);
        Article found = article.filter(a -> a.getStatus() == ArticleStatus.PUBLISHED)
            .orElseThrow(() -> new ArticleNotFoundException(id));
        return ArticleResponse.from(found);
    }

    /**
     * Full-text search across published articles in the effective workspace.
     *
     * @param query    the search query
     * @param page     the page index (zero-based)
     * @param size     the page size
     * @param tagNames optional tag names to filter by
     * @return a page of search results
     */
    public Page<ArticleSearchResult> search(String query, int page, int size, List<String> tagNames) {
        var pageRequest = PageRequest.of(page, size);
        var normalizedTags = tagNames == null
            ? List.<String>of()
            : tagNames.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .toList();

        UUID wsId = resolveWorkspaceId();
        var results = wsId != null
            ? (normalizedTags.isEmpty()
                ? articleRepository.searchByText(query, wsId, pageRequest)
                : articleRepository.searchByTextAndTagNames(query, normalizedTags, wsId, pageRequest))
            : (normalizedTags.isEmpty()
                ? articleRepository.searchByText(query, pageRequest)
                : articleRepository.searchByTextAndTagNames(query, normalizedTags, pageRequest));

        List<ArticleSearchResult> items = results.getContent().stream()
            .map(row -> {
                var id = (UUID) row[0];
                var titleEn = (String) row[1];
                var titleFr = (String) row[2];
                var slug = (String) row[3];
                var excerpt = (String) row[4];
                var publishedAt = (LocalDateTime) row[5];
                var headlineEn = (String) row[6];
                var headlineFr = (String) row[7];
                var tagArray = (Object[]) row[8];

                var title = titleEn != null ? titleEn : titleFr;
                var headline = headlineEn != null ? headlineEn : headlineFr;

                var tagsForArticle = tagArray == null
                    ? Set.<String>of()
                    : Arrays.stream(tagArray)
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .filter(t -> !t.isBlank())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                return new ArticleSearchResult(
                    id, title, headline, slug, excerpt, publishedAt, tagsForArticle);
            })
            .toList();

        return new PageImpl<>(items, pageRequest, results.getTotalElements());
    }

    /**
     * Retrieves tag facets for published articles in the effective workspace.
     *
     * @return the list of tag search facets
     */
    public List<ArticleSearchTag> getSearchTags() {
        UUID wsId = resolveWorkspaceId();
        var facets = wsId != null
            ? articleRepository.findPublishedTagFacets(wsId)
            : articleRepository.findPublishedTagFacets();
        return facets.stream()
            .map(row -> new ArticleSearchTag(
                (String) row[0],
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).longValue()
            ))
            .toList();
    }
}
