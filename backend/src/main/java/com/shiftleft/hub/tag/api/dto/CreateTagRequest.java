package com.shiftleft.hub.tag.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTagRequest(
    @NotBlank String nameEn,
    @NotBlank String nameFr,
    @NotBlank String color
) {}
