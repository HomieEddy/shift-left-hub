package com.shiftleft.hub.tag.api.dto;

import com.shiftleft.hub.tag.domain.Tag;
import java.time.LocalDateTime;
import java.util.UUID;

public record TagResponse(
    UUID id,
    String nameEn,
    String nameFr,
    String color,
    long articleCount,
    LocalDateTime createdAt
) {
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
