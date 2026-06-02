package com.shiftleft.hub.tag.api.dto;

import com.shiftleft.hub.tag.domain.Tag;
import java.util.UUID;

public record TagResponse(
    UUID id,
    String nameEn,
    String nameFr,
    String color
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(
            tag.getId(),
            tag.getNameEn(),
            tag.getNameFr(),
            tag.getColor()
        );
    }
}
