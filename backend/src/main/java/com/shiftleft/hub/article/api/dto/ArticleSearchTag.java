package com.shiftleft.hub.article.api.dto;

/**
 * DTO for tag facets shown in the public article search.
 */
public record ArticleSearchTag(
    String nameEn,
    String nameFr,
    String color,
    long articleCount
) {
}
