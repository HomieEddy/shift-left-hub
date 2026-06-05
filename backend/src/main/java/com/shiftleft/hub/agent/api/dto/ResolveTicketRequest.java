package com.shiftleft.hub.agent.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for resolving a ticket.
 *
 * @param resolutionNotes description of the resolution steps (must not be blank)
 * @param isKnowledgeGap  whether this ticket should be flagged for KCS article drafting
 */
public record ResolveTicketRequest(
    @NotBlank String resolutionNotes,
    boolean isKnowledgeGap
) {
}
