package com.shiftleft.hub.article.service;

import com.shiftleft.hub.article.api.dto.ArticleResponse;
import com.shiftleft.hub.article.api.dto.ArticleSearchResult;
import com.shiftleft.hub.article.domain.ArticleNotFoundException;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicArticleService {

    private final ArticleRepository articleRepository;

    public Page<ArticleResponse> getPublishedArticles(int page, int size) {
        return articleRepository.findByStatus(
                ArticleStatus.PUBLISHED,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt")))
            .map(ArticleResponse::from);
    }

    public ArticleResponse getPublishedArticleById(UUID id) {
        var article = articleRepository.findById(id)
            .orElseThrow(() -> new ArticleNotFoundException(id));
        if (article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new ArticleNotFoundException(id);
        }
        return ArticleResponse.from(article);
    }

    public Page<ArticleSearchResult> search(String query, int page, int size) {
        var pageRequest = PageRequest.of(page, size);
        var results = articleRepository.searchByText(query, pageRequest);

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

                // Return English headline by default; frontend can switch
                var title = titleEn != null ? titleEn : titleFr;
                var headline = headlineEn != null ? headlineEn : headlineFr;

                return new ArticleSearchResult(
                    id, title, headline, slug, excerpt, publishedAt, Set.of());
            })
            .toList();

        return new PageImpl<>(items, pageRequest, results.getTotalElements());
    }
}
