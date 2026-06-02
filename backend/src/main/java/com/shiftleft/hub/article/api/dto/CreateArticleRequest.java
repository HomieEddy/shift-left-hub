package com.shiftleft.hub.article.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;

public record CreateArticleRequest(
    @NotBlank String titleEn,
    @NotBlank String contentEn,
    String titleFr,
    String contentFr,
    String excerpt,
    String featuredImage,
    Set<UUID> tagIds
) {}
