package com.shiftleft.hub.tag.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for updating an existing tag.
 */
public record UpdateTagRequest(
    @NotBlank String nameEn,
    @NotBlank String nameFr,
    @NotBlank String color
) {
}
