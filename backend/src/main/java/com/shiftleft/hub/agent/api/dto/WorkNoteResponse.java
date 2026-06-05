package com.shiftleft.hub.agent.api.dto;

import com.shiftleft.hub.agent.domain.WorkNote;
import java.time.LocalDateTime;
import java.util.UUID;

public record WorkNoteResponse(
    UUID id,
    String authorDisplayName,
    String content,
    LocalDateTime createdAt
) {
    public static WorkNoteResponse from(WorkNote workNote) {
        return new WorkNoteResponse(
            workNote.getId(),
            workNote.getAuthor().getDisplayName(),
            workNote.getContent(),
            workNote.getCreatedAt()
        );
    }
}
