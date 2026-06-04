package com.shiftleft.hub.ticket.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketNumberSequenceRepository extends JpaRepository<TicketNumberSequence, Long> {
}
