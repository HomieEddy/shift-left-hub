package com.shiftleft.hub.ticket.api.dto;

import com.shiftleft.hub.ticket.domain.Ticket;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketStatus;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
    UUID id,
    String ticketNumber,
    TicketStatus status,
    TicketCategory category,
    TicketUrgency urgency,
    String issue,
    String shiftLeftContext,
    UUID userId,
    String userDisplayName,
    LocalDateTime resolvedAt,
    LocalDateTime cancelledAt,
    String cancelReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
            ticket.getId(),
            ticket.getTicketNumber(),
            ticket.getStatus(),
            ticket.getCategory(),
            ticket.getUrgency(),
            ticket.getIssue(),
            ticket.getShiftLeftContext(),
            ticket.getUser().getId(),
            ticket.getUser().getDisplayName(),
            ticket.getResolvedAt(),
            ticket.getCancelledAt(),
            ticket.getCancelReason(),
            ticket.getCreatedAt(),
            ticket.getUpdatedAt()
        );
    }
}
