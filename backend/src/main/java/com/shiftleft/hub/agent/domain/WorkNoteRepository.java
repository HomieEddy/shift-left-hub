package com.shiftleft.hub.agent.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link WorkNote} entities.
 */
public interface WorkNoteRepository extends JpaRepository<WorkNote, UUID> {

    /**
     * Finds all work notes for a ticket, ordered by creation time descending.
     *
     * @param ticketId the ticket UUID
     * @return list of work notes, newest first
     */
    List<WorkNote> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);

    /**
     * Finds all work notes for a ticket without ordering guarantees.
     *
     * @param ticketId the ticket UUID
     * @return list of work notes
     */
    List<WorkNote> findByTicketId(UUID ticketId);
}
