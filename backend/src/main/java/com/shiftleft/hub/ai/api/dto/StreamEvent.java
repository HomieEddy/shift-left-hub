package com.shiftleft.hub.ai.api.dto;

import jakarta.annotation.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * SSE stream event sent to the client during chat streaming.
 *
 * @param type    the event type (token, done, error, fallback)
 * @param content the event content
 * @param sources the source article references (nullable)
 */
public record StreamEvent(
    String type,
    String content,
    @Nullable List<SourceRef> sources
) {
    /**
     * A reference to a source article used in generating a response.
     *
     * @param articleId the article UUID
     * @param title     the article title
     * @param slug      the article slug
     * @param score     the relevance score
     */
    public record SourceRef(UUID articleId, String title, String slug, double score,
                             @Nullable String filename, @Nullable String excerpt) {
    }
}
