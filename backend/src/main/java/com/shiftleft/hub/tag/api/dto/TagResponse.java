package com.shiftleft.hub.tag.api.dto;

import com.shiftleft.hub.tag.domain.Tag;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response payload for tag data.
 */
public record TagResponse(
    UUID id,
    String nameEn,
    String nameFr,
    String color,
    long articleCount,
    LocalDateTime createdAt
) {
    /**
     * Creates a TagResponse from a Tag entity with zero article count.
     *
     * @param tag the tag entity
     * @return the tag response
     */
    public static TagResponse from(Tag tag) {
        return new TagResponse(
            tag.getId(),
            tag.getNameEn(),
            tag.getNameFr(),
            tag.getColor(),
            0L,
            tag.getCreatedAt()
        );
    }

    /**
     * Creates a TagResponse from a Tag entity with the given article count.
     *
     * @param tag          the tag entity
     * @param articleCount the number of articles using this tag
     * @return the tag response
     */
    public static TagResponse from(Tag tag, long articleCount) {
        return new TagResponse(
            tag.getId(),
            tag.getNameEn(),
            tag.getNameFr(),
            tag.getColor(),
            articleCount,
            tag.getCreatedAt()
        );
    }
}
