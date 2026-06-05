package com.shiftleft.hub.kcs.api.dto;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.tag.api.dto.TagResponse;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO for a KCS-drafted article in the admin review queue.
 * <p>Extends the standard article data with the source ticket number
 * and any duplicate/similarity warnings.</p>
 *
 * @param id                 the article UUID
 * @param titleEn            English title
 * @param titleFr            French title
 * @param slug               URL slug
 * @param excerpt            article excerpt
 * @param status             article status (always DRAFT for pending queue)
 * @param sourceTicketId     the source ticket UUID
 * @param sourceTicketNumber the source ticket number (e.g. TKT-0003)
 * @param similarityWarnings list of article titles that are potential duplicates
 * @param tags               article tags
 * @param createdAt          creation timestamp
 */
public record KcsDraftResponse(
    UUID id,
    String titleEn,
    String titleFr,
    String slug,
    String excerpt,
    ArticleStatus status,
    UUID sourceTicketId,
    String sourceTicketNumber,
    Set<String> similarityWarnings,
    Set<TagResponse> tags,
    LocalDateTime createdAt
) {
    public static KcsDraftResponse from(Article article) {
        return new KcsDraftResponse(
            article.getId(),
            article.getTitleEn(),
            article.getTitleFr(),
            article.getSlug(),
            article.getExcerpt(),
            article.getStatus(),
            article.getSourceTicketId(),
            null, // Source ticket number is resolved separately
            Set.of(), // Similarity warnings resolved separately
            article.getTags().stream().map(TagResponse::from).collect(Collectors.toSet()),
            article.getCreatedAt()
        );
    }
}
