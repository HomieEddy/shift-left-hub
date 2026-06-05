package com.shiftleft.hub.agent.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for adding a work note to a ticket.
 *
 * @param content the work note content (must not be blank)
 */
public record AddWorkNoteRequest(
    @NotBlank String content
) {
}
