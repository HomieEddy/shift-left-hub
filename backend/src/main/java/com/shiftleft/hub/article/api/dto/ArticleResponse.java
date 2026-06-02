package com.shiftleft.hub.article.api.dto;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.tag.api.dto.TagResponse;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    Set<TagResponse> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
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
            article.getTags().stream().map(TagResponse::from).collect(Collectors.toSet()),
            article.getCreatedAt(),
            article.getUpdatedAt()
        );
    }
}
