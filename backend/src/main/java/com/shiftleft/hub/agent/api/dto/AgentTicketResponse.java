package com.shiftleft.hub.agent.api.dto;

import com.shiftleft.hub.ticket.domain.Ticket;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketStatus;
import com.shiftleft.hub.ticket.domain.TicketUrgency;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a ticket from the agent's perspective.
 * <p>Contains all ticket fields with resolved display names for
 * assigned-to and resolved-by users. Used as the response type for
 * agent ticket endpoints.</p>
 *
 * @param id                    the ticket UUID
 * @param ticketNumber          the human-readable ticket number
 * @param status                the current ticket status
 * @param category              the ticket category
 * @param urgency               the ticket urgency level
 * @param issue                 the issue description
 * @param shiftLeftContext      JSON shift-left context (nullable)
 * @param userId                the UUID of the requesting user
 * @param userDisplayName       the display name of the requesting user
 * @param userEmail             the email of the requesting user
 * @param assignedToId          the UUID of the assigned agent (nullable)
 * @param assignedToDisplayName the display name of the assigned agent (nullable)
 * @param resolvedById          the UUID of the resolving agent (nullable)
 * @param resolvedByDisplayName the display name of the resolving agent (nullable)
 * @param resolutionNotes       the resolution notes (nullable)
 * @param isKnowledgeGap        whether this ticket should be flagged for KCS
 * @param resolvedAt            the resolution timestamp (nullable)
 * @param cancelledAt           the cancellation timestamp (nullable)
 * @param cancelReason          the cancellation reason (nullable)
 * @param createdAt             the creation timestamp
 * @param updatedAt             the last update timestamp
 */
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
    /**
     * Creates an {@link AgentTicketResponse} from a {@link Ticket} entity.
     *
     * @param ticket the ticket entity (must not be null)
     * @return a new agent ticket response with resolved user display names
     */
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
