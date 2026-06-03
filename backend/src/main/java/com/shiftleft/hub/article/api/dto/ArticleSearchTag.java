package com.shiftleft.hub.article.api.dto;

public record ArticleSearchTag(
    String nameEn,
    String nameFr,
    String color,
    long articleCount
) {}
