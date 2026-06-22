package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Finds similar articles by title-keyword overlap — used to enrich the
 * {@code KcsDraftResponse} with similarity warnings.
 *
 * <p>Single responsibility: return up to 3 article titles whose keywords
 * overlap with the given article's English title. Failures degrade
 * to an empty set so the response can still be served.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KcsSimilaritySearch {

    private static final int SEARCH_LIMIT = 5;
    private static final int RESULT_LIMIT = 3;

    private final ArticleRepository articleRepository;

    /**
     * Returns up to 3 similar article titles (excluding the input article).
     *
     * @param article the article to find similar matches for
     * @return set of similar article titles, never null
     */
    public Set<String> findSimilarArticles(Article article) {
        if (article.getTitleEn() == null || article.getTitleEn().isBlank()) {
            return Set.of();
        }
        String keywords = article.getTitleEn().toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (keywords.isEmpty()) {
            return Set.of();
        }
        try {
            UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
            var results = articleRepository.searchByText(keywords, workspaceId, PageRequest.of(0, SEARCH_LIMIT));
            List<Object[]> rows = results.getContent().stream()
                .map(row -> (Object[]) row)
                .toList();
            return rows.stream()
                .filter(row -> {
                    UUID id = (UUID) row[0];
                    return !id.equals(article.getId());
                })
                .map(row -> (String) row[1])
                .filter(Objects::nonNull)
                .limit(RESULT_LIMIT)
                .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Similar article check failed: {}", e.getMessage());
            return Set.of();
        }
    }
}
