package com.shiftleft.hub.article.api;

import com.shiftleft.hub.article.api.dto.ArticleResponse;
import com.shiftleft.hub.article.api.dto.ArticleSearchResult;
import com.shiftleft.hub.article.api.dto.ArticleSearchTag;
import com.shiftleft.hub.article.service.PublicArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class PublicArticleController {

    private static final int MAX_PAGE_SIZE = 100;

    private final PublicArticleService publicArticleService;

    /**
     * Lists published articles with pagination.
     *
     * @param page the page index (zero-based)
     * @param size the page size
     * @return a page of article responses
     */
    @GetMapping
    public Page<ArticleResponse> listArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return publicArticleService.getPublishedArticles(safePage, safeSize);
    }

    /**
     * Full-text search across published articles.
     *
     * @param q    the search query
     * @param tags optional tag names to filter by
     * @param page the page index (zero-based)
     * @param size the page size
     * @return a page of search results
     */
    @GetMapping("/search")
    public Page<ArticleSearchResult> searchArticles(
            @RequestParam String q,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return publicArticleService.search(q, safePage, safeSize, tags);
    }

    /**
     * Retrieves tag facets for use in search filtering.
     *
     * @return the list of available search tags
     */
    @GetMapping("/search/tags")
    public List<ArticleSearchTag> searchTags() {
        return publicArticleService.getSearchTags();
    }

    /**
     * Retrieves a published article by its ID.
     *
     * @param id the article UUID
     * @return the article response
     */
    @GetMapping("/{id}")
    public ArticleResponse getArticle(@PathVariable UUID id) {
        return publicArticleService.getPublishedArticleById(id);
    }
}
