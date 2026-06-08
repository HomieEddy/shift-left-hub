package com.shiftleft.hub.article.domain;

import java.util.UUID;

/**
 * Thrown when an article is not found by its ID.
 */
public class ArticleNotFoundException extends RuntimeException {

    /**
     * Creates a new ArticleNotFoundException for the given ID.
     *
     * @param id the article UUID that was not found
     */
    public ArticleNotFoundException(UUID id) {
        super("Article not found: " + id);
    }
}
