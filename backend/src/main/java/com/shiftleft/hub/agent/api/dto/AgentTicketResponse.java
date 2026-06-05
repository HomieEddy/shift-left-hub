package com.shiftleft.hub.agent.api.dto;

import com.shiftleft.hub.ticket.domain.Ticket;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketStatus;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import java.time.LocalDateTime;
import java.util.UUID;

public record AgentTicketResponse(
    UUID id,
    String ticketNumber,
    TicketStatus status,
    TicketCategory category,
    TicketUrgency urgency,
    String issue,
    String shiftLeftContext,
    UUID userId,
    String userDisplayName,
    String userEmail,
    UUID assignedToId,
    String assignedToDisplayName,
    UUID resolvedById,
    String resolvedByDisplayName,
    String resolutionNotes,
    boolean isKnowledgeGap,
    LocalDateTime resolvedAt,
    LocalDateTime cancelledAt,
    String cancelReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AgentTicketResponse from(Ticket ticket) {
        return new AgentTicketResponse(
            ticket.getId(),
            ticket.getTicketNumber(),
            ticket.getStatus(),
            ticket.getCategory(),
            ticket.getUrgency(),
            ticket.getIssue(),
            ticket.getShiftLeftContext(),
            ticket.getUser().getId(),
            ticket.getUser().getDisplayName(),
            ticket.getUser().getEmail(),
            ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null,
            ticket.getAssignedTo() != null ? ticket.getAssignedTo().getDisplayName() : null,
            ticket.getResolvedBy() != null ? ticket.getResolvedBy().getId() : null,
            ticket.getResolvedBy() != null ? ticket.getResolvedBy().getDisplayName() : null,
            ticket.getResolutionNotes(),
            ticket.isKnowledgeGap(),
            ticket.getResolvedAt(),
            ticket.getCancelledAt(),
            ticket.getCancelReason(),
            ticket.getCreatedAt(),
            ticket.getUpdatedAt()
        );
    }
}
