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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;
    private final EmbeddingService embeddingService;

    /**
     * Retrieves an article by its ID.
     *
     * @param id the article UUID
     * @return the article response
     */
    public ArticleResponse getArticleById(UUID id) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new ArticleNotFoundException(id));
        if (article.getWorkspaceId() != null
            && WorkspaceContextHolder.hasCurrentWorkspaceId()
            && !article.getWorkspaceId().equals(WorkspaceContextHolder.getCurrentWorkspaceId())) {
            throw new ArticleNotFoundException(id);
        }
        return ArticleResponse.from(article);
    }

    /**
     * Retrieves all articles with pagination.
     *
     * @param page the page index (zero-based)
     * @param size the page size
     * @return a page of article responses
     */
    public Page<ArticleResponse> getAllArticles(int page, int size) {
        return articleRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
            .map(ArticleResponse::from);
    }

    /**
     * Retrieves articles filtered by their status.
     *
     * @param status the article status to filter by
     * @param page   the page index (zero-based)
     * @param size   the page size
     * @return a page of article responses
     */
    public Page<ArticleResponse> getArticlesByStatus(ArticleStatus status, int page, int size) {
        return articleRepository.findByStatus(status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
            .map(ArticleResponse::from);
    }

    /**
     * Creates a new article.
     *
     * @param request the create article request
     * @param author  the authoring user
     * @return the created article response
     */
    @Transactional
    public ArticleResponse createArticle(CreateArticleRequest request, User author) {
        if (request.titleEn() == null || request.titleEn().isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        if (request.contentEn() == null || request.contentEn().isBlank()) {
            throw new IllegalArgumentException("Content must not be blank");
        }
        Set<Tag> tags = resolveTags(request.tagIds());

        String slug = slugify(request.titleEn());
        if (articleRepository.findBySlug(slug).isPresent()) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        Article article = Article.builder()
            .titleEn(request.titleEn())
            .contentEn(request.contentEn())
            .titleFr(request.titleFr())
            .contentFr(request.contentFr())
            .slug(slug)
            .excerpt(request.excerpt())
            .excerptFr(request.excerptFr())
            .featuredImage(request.featuredImage())
            .status(ArticleStatus.DRAFT)
            .viewCount(0)
            .author(author)
            .tags(tags)
            .build();

        article = articleRepository.save(article);
        return ArticleResponse.from(article);
    }

    /**
     * Updates an existing article.
     *
     * @param id      the article UUID
     * @param request the update article request
     * @param editor  the editing user
     * @return the updated article response
     */
    @Transactional
    public ArticleResponse updateArticle(UUID id, UpdateArticleRequest request, User editor) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new ArticleNotFoundException(id));

        article.setTitleEn(request.titleEn());
        String newSlug = slugify(request.titleEn());
        if (!newSlug.equals(article.getSlug()) && articleRepository.findBySlug(newSlug).isPresent()) {
            newSlug = newSlug + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        article.setSlug(newSlug);
        article.setContentEn(request.contentEn());
        article.setTitleFr(request.titleFr());
        article.setContentFr(request.contentFr());
        article.setExcerpt(request.excerpt());
        article.setExcerptFr(request.excerptFr());
        article.setFeaturedImage(request.featuredImage());
        article.setLastEditor(editor);
        article.setTags(resolveTags(request.tagIds()));

        article = articleRepository.save(article);
        return ArticleResponse.from(article);
    }

    /**
     * Publishes an article by its ID.
     *
     * @param id the article UUID
     * @return the published article response
     */
    @Transactional
    public ArticleResponse publishArticle(UUID id) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new ArticleNotFoundException(id));
        article.setStatus(ArticleStatus.PUBLISHED);
        if (article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }
        article = articleRepository.save(article);

        try {
            embeddingService.generateAndStoreEmbedding(article);
        } catch (Exception e) {
            log.warn("Failed to generate embedding for article {}: {}", id, e.getMessage());
        }

        return ArticleResponse.from(article);
    }

    /**
     * Archives an article by its ID.
     *
     * @param id the article UUID
     * @return the archived article response
     */
    @Transactional
    public ArticleResponse archiveArticle(UUID id) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new ArticleNotFoundException(id));
        if (article.getStatus() == ArticleStatus.ARCHIVED) {
            return ArticleResponse.from(article);
        }
        article.setStatus(ArticleStatus.ARCHIVED);
        article = articleRepository.save(article);
        return ArticleResponse.from(article);
    }

    /**
     * Deletes an article by its ID.
     *
     * @param id the article UUID
     */
    @Transactional
    public void deleteArticle(UUID id) {
        if (!articleRepository.existsById(id)) {
            throw new ArticleNotFoundException(id);
        }
        articleRepository.deleteById(id);
    }

    private Set<Tag> resolveTags(Set<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Tag> foundTags = tagRepository.findAllById(tagIds);
        if (foundTags.size() != tagIds.size()) {
            Set<UUID> foundIds = foundTags.stream().map(Tag::getId).collect(Collectors.toSet());
            Set<UUID> missing = new HashSet<>(tagIds);
            missing.removeAll(foundIds);
            throw new TagNotFoundException(missing.iterator().next());
        }
        return new HashSet<>(foundTags);
    }

    private String slugify(String title) {
        return title.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}
