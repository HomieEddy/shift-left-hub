package com.shiftleft.hub.article.api;

import com.shiftleft.hub.article.api.dto.ArticleResponse;
import com.shiftleft.hub.article.api.dto.ArticleSearchResult;
import com.shiftleft.hub.article.service.PublicArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class PublicArticleController {

    private final PublicArticleService publicArticleService;

    @GetMapping
    public Page<ArticleResponse> listArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return publicArticleService.getPublishedArticles(page, size);
    }

    @GetMapping("/search")
    public Page<ArticleSearchResult> searchArticles(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return publicArticleService.search(q, page, size);
    }

    @GetMapping("/{id}")
    public ArticleResponse getArticle(@PathVariable UUID id) {
        return publicArticleService.getPublishedArticleById(id);
    }
}
