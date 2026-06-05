package com.shiftleft.hub.agent.service;

import com.shiftleft.hub.agent.api.dto.AgentTicketResponse;
import com.shiftleft.hub.agent.api.dto.WorkNoteResponse;
import com.shiftleft.hub.agent.domain.WorkNote;
import com.shiftleft.hub.agent.domain.WorkNoteRepository;
import com.shiftleft.hub.kcs.domain.TicketResolvedEvent;
import com.shiftleft.hub.ticket.domain.Ticket;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketNotFoundException;
import com.shiftleft.hub.ticket.domain.TicketRepository;
import com.shiftleft.hub.ticket.domain.TicketStatus;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for agent ticket operations.
 * <p>Handles ticket listing, claiming, work notes, and resolution workflows.
 * All public mutating methods are {@code @Transactional} to ensure data
 * consistency. Read operations use {@code readOnly = true} at the class level.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AgentTicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final WorkNoteRepository workNoteRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Lists tickets with optional server-side filtering.
     *
     * @param status   filter by ticket status (nullable)
     * @param category filter by ticket category (nullable)
     * @param urgency  filter by ticket urgency (nullable)
     * @param search   full-text search on ticket number or user display name (nullable)
     * @return sorted list of agent ticket responses
     */
    public List<AgentTicketResponse> listTickets(
            TicketStatus status, TicketCategory category, TicketUrgency urgency, String search) {

        return ticketRepository.findAll().stream()
            .filter(t -> status == null || t.getStatus() == status)
            .filter(t -> category == null || t.getCategory() == category)
            .filter(t -> urgency == null || t.getUrgency() == urgency)
            .filter(t -> search == null || search.isBlank()
                || t.getTicketNumber().toLowerCase().contains(search.toLowerCase())
                || t.getUser().getDisplayName().toLowerCase().contains(search.toLowerCase()))
            .sorted(agentTicketComparator())
            .map(AgentTicketResponse::from)
            .toList();
    }

    /**
     * Retrieves full ticket detail by id.
     *
     * @param ticketId the ticket UUID
     * @return agent ticket response with assignment and resolution fields
     * @throws TicketNotFoundException if ticket does not exist
     */
    public AgentTicketResponse getTicketDetail(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
        return AgentTicketResponse.from(ticket);
    }

    /**
     * Claims a NEW ticket for the given agent.
     * <p>Transitions status from NEW to IN_PROGRESS and assigns the agent.
     * Uses pessimistic write locking to prevent concurrent claims.</p>
     *
     * @param ticketId   the ticket UUID
     * @param agentEmail email of the claiming agent
     * @return updated ticket response
     * @throws IllegalStateException if ticket is not in NEW status
     */
    @Transactional
    public AgentTicketResponse claimTicket(UUID ticketId, String agentEmail) {
        User agent = getUserByEmail(agentEmail);
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (ticket.getStatus() != TicketStatus.NEW) {
            throw new IllegalStateException(
                "Cannot claim ticket " + ticket.getTicketNumber() + ": status is " + ticket.getStatus());
        }

        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket = ticketRepository.save(ticket);
        log.info("Ticket {} claimed by agent {}", ticket.getTicketNumber(), agentEmail);
        return AgentTicketResponse.from(ticket);
    }

    /**
     * Adds a work note to a ticket.
     *
     * @param ticketId   the ticket UUID
     * @param agentEmail email of the agent adding the note
     * @param content    the note content
     * @return created work note response
     */
    @Transactional
    public WorkNoteResponse addWorkNote(UUID ticketId, String agentEmail, String content) {
        User agent = getUserByEmail(agentEmail);
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));

        WorkNote note = WorkNote.builder()
            .ticket(ticket)
            .author(agent)
            .content(content)
            .build();
        note = workNoteRepository.saveAndFlush(note);
        log.info("Work note added to ticket {} by {}", ticket.getTicketNumber(), agentEmail);
        return WorkNoteResponse.from(note);
    }

    /**
     * Retrieves work notes for a ticket in reverse chronological order.
     *
     * @param ticketId the ticket UUID
     * @return list of work note responses, newest first
     */
    public List<WorkNoteResponse> getWorkNotes(UUID ticketId) {
        return workNoteRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
            .stream()
            .map(WorkNoteResponse::from)
            .toList();
    }

    /**
     * Resolves an IN_PROGRESS ticket with resolution notes.
     * <p>Validates that the resolving agent is the one who claimed the ticket.
     * Transitions status to RESOLVED and records the resolution timestamp.</p>
     *
     * @param ticketId        the ticket UUID
     * @param agentEmail      email of the resolving agent
     * @param resolutionNotes description of resolution steps
     * @param isKnowledgeGap  whether this should be flagged for KCS drafting
     * @return updated ticket response
     * @throws IllegalStateException if ticket is not IN_PROGRESS or assigned to another agent
     */
    @Transactional
    public AgentTicketResponse resolveTicket(UUID ticketId, String agentEmail,
            String resolutionNotes, boolean isKnowledgeGap) {
        User agent = getUserByEmail(agentEmail);
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Cannot resolve ticket " + ticket.getTicketNumber() + ": status is " + ticket.getStatus());
        }

        if (ticket.getAssignedTo() == null || !ticket.getAssignedTo().getId().equals(agent.getId())) {
            throw new IllegalStateException(
                "Ticket " + ticket.getTicketNumber() + " is assigned to another agent");
        }

        ticket.setResolvedBy(agent);
        ticket.setResolutionNotes(resolutionNotes);
        ticket.setKnowledgeGap(isKnowledgeGap);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        // Publish KCS drafting event if flagged as knowledge gap (per D-01, T-06-01)
        if (isKnowledgeGap) {
            try {
                eventPublisher.publishEvent(new TicketResolvedEvent(
                    ticket.getId(),
                    ticket.getTicketNumber(),
                    ticket.getIssue(),
                    ticket.getShiftLeftContext(),
                    ticket.getCategory(),
                    ticket.getUrgency(),
                    ticket.getResolutionNotes(),
                    ticket.getUser() != null ? ticket.getUser().getDisplayName() : "unknown",
                    ticket.getUser() != null ? ticket.getUser().getEmail() : "",
                    agent.getDisplayName(),
                    ticket.getResolvedAt()
                ));
                log.info("TicketResolvedEvent published for ticket {} (KCS)", ticket.getTicketNumber());
            } catch (Exception e) {
                log.error("Failed to publish TicketResolvedEvent for ticket {}: {}",
                    ticket.getTicketNumber(), e.getMessage());
                // Non-blocking — ticket resolution is already saved
            }
        }

        log.info("Ticket {} resolved by agent {} (KCS: {})",
            ticket.getTicketNumber(), agentEmail, isKnowledgeGap);
        return AgentTicketResponse.from(ticket);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private Comparator<Ticket> agentTicketComparator() {
        return Comparator
            .<Ticket, Integer>comparing(t -> statusOrder(t.getStatus()))
            .thenComparing(t -> urgencyOrder(t.getUrgency()))
            .thenComparing(Ticket::getCreatedAt, Comparator.reverseOrder());
    }

    private int statusOrder(TicketStatus status) {
        return switch (status) {
            case NEW -> 0;
            case IN_PROGRESS -> 1;
            case RESOLVED -> 2;
            case CANCELLED -> 3;
        };
    }

    private int urgencyOrder(TicketUrgency urgency) {
        return switch (urgency) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }
}
