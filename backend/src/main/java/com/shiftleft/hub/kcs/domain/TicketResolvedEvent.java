package com.shiftleft.hub.kcs.domain;

import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketUrgency;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event fired when an agent resolves a ticket that is flagged as a Knowledge Gap.
 * <p>Carries all the data the KCS drafting listener needs so it can
 * draft without issuing additional database queries. (D-03)</p>
 *
 * @param ticketId        the resolved ticket UUID
 * @param ticketNumber    the human-readable ticket number (e.g. TKT-0003)
 * @param issue           the user's original issue description
 * @param shiftLeftContext the JSON shift-left context (AI transcript + sources)
 * @param category        the ticket category
 * @param urgency         the ticket urgency level
 * @param resolutionNotes the agent's resolution notes
 * @param userDisplayName the display name of the ticket creator
 * @param userEmail       the email of the ticket creator
 * @param agentDisplayName the display name of the resolving agent
 * @param resolvedAt      the resolution timestamp
 */
public record TicketResolvedEvent(
    UUID ticketId,
    String ticketNumber,
    String issue,
    String shiftLeftContext,
    TicketCategory category,
    TicketUrgency urgency,
    String resolutionNotes,
    String userDisplayName,
    String userEmail,
    String agentDisplayName,
    LocalDateTime resolvedAt
) {
}
