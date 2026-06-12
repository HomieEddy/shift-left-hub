package com.shiftleft.hub.category.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CategoryRequest(
    @NotBlank String nameEn,
    @NotBlank String nameFr,
    UUID parentId
) {
}