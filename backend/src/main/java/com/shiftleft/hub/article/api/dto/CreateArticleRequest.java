package com.shiftleft.hub.article.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

/**
 * Request payload for creating a new article.
 */
public record CreateArticleRequest(
    @NotBlank String titleEn,
    @NotBlank String contentEn,
    String titleFr,
    String contentFr,
    String excerpt,
    String excerptFr,
    String featuredImage,
    Set<UUID> tagIds
) {
}
