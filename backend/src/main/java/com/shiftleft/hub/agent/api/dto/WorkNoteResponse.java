package com.shiftleft.hub.agent.api.dto;

import com.shiftleft.hub.agent.domain.WorkNote;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a work note response.
 * <p>Contains the note content along with the author's display name
 * and creation timestamp. Used as the response type for work note
 * endpoints.</p>
 *
 * @param id                the work note UUID
 * @param authorDisplayName the display name of the note author
 * @param content           the note content
 * @param createdAt         the creation timestamp
 */
public record WorkNoteResponse(
    UUID id,
    String authorDisplayName,
    String content,
    LocalDateTime createdAt
) {
    /**
     * Creates a {@link WorkNoteResponse} from a {@link WorkNote} entity.
     *
     * @param workNote the work note entity (must not be null)
     * @return a new work note response with author display name resolved
     */
    public static WorkNoteResponse from(WorkNote workNote) {
        return new WorkNoteResponse(
            workNote.getId(),
            workNote.getAuthor().getDisplayName(),
            workNote.getContent(),
            workNote.getCreatedAt()
        );
    }
}
