package com.shiftleft.hub.article.api.dto;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.tag.api.dto.TagResponse;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response payload for article data.
 */
public record ArticleResponse(
    UUID id,
    String titleEn,
    String contentEn,
    String titleFr,
    String contentFr,
    String slug,
    String excerpt,
    String featuredImage,
    ArticleStatus status,
    int viewCount,
    LocalDateTime publishedAt,
    UUID authorId,
    String authorName,
    UUID lastEditorId,
    String lastEditorName,
    Set<TagResponse> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Creates an ArticleResponse from an Article entity.
     *
     * @param article the article entity
     * @return the article response
     */
    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
            article.getId(),
            article.getTitleEn(),
            article.getContentEn(),
            article.getTitleFr(),
            article.getContentFr(),
            article.getSlug(),
            article.getExcerpt(),
            article.getFeaturedImage(),
            article.getStatus(),
            article.getViewCount(),
            article.getPublishedAt(),
            article.getAuthor().getId(),
            article.getAuthor().getDisplayName(),
            article.getLastEditor() != null ? article.getLastEditor().getId() : null,
            article.getLastEditor() != null ? article.getLastEditor().getDisplayName() : null,
            article.getTags().stream().map(TagResponse::from).collect(Collectors.toSet()),
            article.getCreatedAt(),
            article.getUpdatedAt()
        );
    }
}
