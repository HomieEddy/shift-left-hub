package com.shiftleft.hub.agent.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkNoteRepository extends JpaRepository<WorkNote, UUID> {

    List<WorkNote> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);

    List<WorkNote> findByTicketId(UUID ticketId);
}
