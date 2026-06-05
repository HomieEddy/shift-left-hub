package com.shiftleft.hub.agent.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveTicketRequest(
    @NotBlank String resolutionNotes,
    boolean isKnowledgeGap
) {
}
