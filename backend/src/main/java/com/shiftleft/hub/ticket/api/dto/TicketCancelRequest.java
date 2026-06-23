package com.shiftleft.hub.ticket.api.dto;

import jakarta.validation.constraints.Size;

/** Request payload for cancelling a ticket. */
public record TicketCancelRequest(
    @Size(max = 500, message = "cancelReason must be at most 500 characters")
    String cancelReason
) {
}
