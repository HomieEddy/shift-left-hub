package com.shiftleft.hub.tag.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for creating a new tag.
 */
public record CreateTagRequest(
    @NotBlank String nameEn,
    @NotBlank String nameFr,
    @NotBlank String color
) {
}
