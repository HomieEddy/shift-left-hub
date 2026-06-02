package com.shiftleft.hub.article.api.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record ArticleSearchResult(
    UUID id,
    String title,
    String headline,
    String slug,
    String excerpt,
    LocalDateTime publishedAt,
    Set<String> tagNames
) {}
