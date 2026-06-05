package com.shiftleft.hub.agent.service;

import com.shiftleft.hub.agent.api.dto.AddWorkNoteRequest;
import com.shiftleft.hub.agent.api.dto.AgentTicketResponse;
import com.shiftleft.hub.agent.api.dto.ResolveTicketRequest;
import com.shiftleft.hub.agent.api.dto.WorkNoteResponse;
import com.shiftleft.hub.agent.domain.WorkNote;
import com.shiftleft.hub.agent.domain.WorkNoteRepository;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AgentTicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final WorkNoteRepository workNoteRepository;

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

    public AgentTicketResponse getTicketDetail(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));
        return AgentTicketResponse.from(ticket);
    }

    @Transactional
    public AgentTicketResponse claimTicket(UUID ticketId, String agentEmail) {
        User agent = getUserByEmail(agentEmail);
        Ticket ticket = ticketRepository.findById(ticketId)
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

    public List<WorkNoteResponse> getWorkNotes(UUID ticketId) {
        return workNoteRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
            .stream()
            .map(WorkNoteResponse::from)
            .toList();
    }

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

        ticket.setResolvedBy(agent);
        ticket.setResolutionNotes(resolutionNotes);
        ticket.setKnowledgeGap(isKnowledgeGap);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);
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
