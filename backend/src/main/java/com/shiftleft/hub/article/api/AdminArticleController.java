package com.shiftleft.hub.article.api;

import com.shiftleft.hub.article.api.dto.ArticleResponse;
import com.shiftleft.hub.article.api.dto.CreateArticleRequest;
import com.shiftleft.hub.article.api.dto.UpdateArticleRequest;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.article.service.ArticleService;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ArticleService articleService;
    private final UserRepository userRepository;

    @GetMapping
    public Page<ArticleResponse> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ArticleStatus status) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        if (status != null) {
            return articleService.getArticlesByStatus(status, safePage, safeSize);
        }
        return articleService.getAllArticles(safePage, safeSize);
    }

    @GetMapping("/{id}")
    public ArticleResponse getArticle(@PathVariable UUID id) {
        return articleService.getArticleById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse createArticle(
            @Valid @RequestBody CreateArticleRequest request,
            Authentication auth) {
        User author = getUserFromAuth(auth);
        return articleService.createArticle(request, author);
    }

    @PutMapping("/{id}")
    public ArticleResponse updateArticle(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateArticleRequest request,
            Authentication auth) {
        User editor = getUserFromAuth(auth);
        return articleService.updateArticle(id, request, editor);
    }

    @PutMapping("/{id}/publish")
    public ArticleResponse publishArticle(@PathVariable UUID id) {
        return articleService.publishArticle(id);
    }

    @PutMapping("/{id}/archive")
    public ArticleResponse archiveArticle(@PathVariable UUID id) {
        return articleService.archiveArticle(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable UUID id) {
        articleService.deleteArticle(id);
    }

    private User getUserFromAuth(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + auth.getName()));
    }
}
