package com.shiftleft.hub.agent.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AddWorkNoteRequest(
    @NotBlank String content
) {
}
