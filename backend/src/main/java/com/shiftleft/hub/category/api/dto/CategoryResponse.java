package com.shiftleft.hub.category.api.dto;

import com.shiftleft.hub.category.domain.Category;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategoryResponse(
    UUID id,
    String nameEn,
    String nameFr,
    UUID parentId,
    long childCount,
    LocalDateTime createdAt
) {
    public static CategoryResponse from(Category category, long childCount) {
        return new CategoryResponse(
            category.getId(),
            category.getNameEn(),
            category.getNameFr(),
            category.getParent() != null ? category.getParent().getId() : null,
            childCount,
            category.getCreatedAt()
        );
    }
}