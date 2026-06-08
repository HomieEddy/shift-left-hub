package com.shiftleft.hub.ai.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request DTO for sending a chat message.
 *
 * @param message the chat message text
 * @param history the conversation history (nullable)
 */
public record ChatRequest(
    @NotBlank String message,
    @Nullable List<ChatMessage> history
) {
    /**
     * A single chat message with role and content.
     *
     * @param role    the message role (user or assistant)
     * @param content the message content
     */
    public record ChatMessage(String role, String content) {
    }
}
