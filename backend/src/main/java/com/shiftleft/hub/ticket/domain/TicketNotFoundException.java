package com.shiftleft.hub.ticket.domain;

import java.util.UUID;

/** Thrown when a ticket is not found by the given ID. */
public class TicketNotFoundException extends RuntimeException {

    /**
     * Creates a new exception for the given ticket ID.
     *
     * @param id the ticket UUID that was not found
     */
    public TicketNotFoundException(UUID id) {
        super("Ticket not found: " + id);
    }
}
