package com.shiftleft.hub.category.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MergeCategoriesRequest(
    @NotNull UUID sourceCategoryId,
    @NotNull UUID targetCategoryId
) {
}