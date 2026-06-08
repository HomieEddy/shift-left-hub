package com.shiftleft.hub.ticket.api.dto;

/** Request payload for cancelling a ticket. */
public record TicketCancelRequest(
    String cancelReason
) {
}
