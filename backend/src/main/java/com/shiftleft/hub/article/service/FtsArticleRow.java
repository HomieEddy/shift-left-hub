package com.shiftleft.hub.article.service;

import java.util.UUID;

/**
 * Typed reader for the first 5 columns of the
 * {@code searchByText} / {@code searchByTextAndTagNames} Object[] row
 * returned by {@code ArticleRepository}. Both {@code AiChatService}
 * (5-col) and {@code PublicArticleService} (9-col) unwrap the same
 * positional columns; this record gives the 5-col subset a name
 * and a factory and lets the public-search service compose it with
 * the 4 extra columns it needs.
 */
public record FtsArticleRow(
    UUID id,
    String titleEn,
    String titleFr,
    String slug,
    String excerpt) {

    /**
     * Reads the first 5 columns of a {@code searchByText} /
     * {@code searchByTextAndTagNames} row.
     *
     * @param row the raw {@code Object[]} returned by the repository
     * @return the typed 5-col subset
     */
    public static FtsArticleRow from(Object[] row) {
        return new FtsArticleRow(
            (UUID) row[0],
            (String) row[1],
            (String) row[2],
            (String) row[3],
            (String) row[4]);
    }
}
