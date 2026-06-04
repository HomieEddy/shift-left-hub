package com.shiftleft.hub.ticket.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Ticket> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, TicketStatus status);

    long countByStatus(TicketStatus status);
}
