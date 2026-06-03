package com.shiftleft.hub.ai.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChatRequest(
    @NotBlank String message,
    @Nullable List<ChatMessage> history
) {
    public record ChatMessage(String role, String content) {}
}
