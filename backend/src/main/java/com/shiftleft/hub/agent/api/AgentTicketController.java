package com.shiftleft.hub.agent.api;

import com.shiftleft.hub.agent.api.dto.AddWorkNoteRequest;
import com.shiftleft.hub.agent.api.dto.AgentTicketResponse;
import com.shiftleft.hub.agent.api.dto.ResolveTicketRequest;
import com.shiftleft.hub.agent.api.dto.WorkNoteResponse;
import com.shiftleft.hub.agent.service.AgentTicketService;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketStatus;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for agent ticket operations.
 * <p>Provides endpoints for listing, claiming, working on, and resolving
 * tickets from the agent's perspective. All endpoints are scoped under
 * {@code /api/agent/tickets}.</p>
 */
@RestController
@RequestMapping("/api/agent/tickets")
@RequiredArgsConstructor
public class AgentTicketController {

    private final AgentTicketService agentTicketService;

    /**
     * Lists tickets with optional server-side filtering.
     *
     * @param status   filter by ticket status (nullable)
     * @param category filter by ticket category (nullable)
     * @param urgency  filter by ticket urgency (nullable)
     * @param search   full-text search on ticket number or user display name (nullable)
     * @return list of agent ticket responses sorted by status, urgency, and recency
     */
    @GetMapping
    public List<AgentTicketResponse> listTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) TicketUrgency urgency,
            @RequestParam(required = false) String search) {
        return agentTicketService.listTickets(status, category, urgency, search);
    }

    /**
     * Retrieves full detail for a single ticket.
     *
     * @param id the ticket UUID
     * @return agent ticket response with assignment and resolution fields
     */
    @GetMapping("/{id}")
    public AgentTicketResponse getTicketDetail(@PathVariable UUID id) {
        return agentTicketService.getTicketDetail(id);
    }

    /**
     * Claims a NEW ticket for the authenticated agent.
     * <p>Transitions the ticket from {@code NEW} to {@code IN_PROGRESS} and
     * assigns it to the calling agent. Uses pessimistic write locking to
     * prevent concurrent claims.</p>
     *
     * @param id   the ticket UUID
     * @param auth the authenticated principal
     * @return updated ticket response
     */
    @PostMapping("/{id}/claim")
    public AgentTicketResponse claimTicket(@PathVariable UUID id, Authentication auth) {
        return agentTicketService.claimTicket(id, auth.getName());
    }

    /**
     * Adds a work note to a ticket.
     *
     * @param id      the ticket UUID
     * @param request the request body containing the note content
     * @param auth    the authenticated principal
     * @return created work note response
     */
    @PostMapping("/{id}/work-notes")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkNoteResponse addWorkNote(
            @PathVariable UUID id,
            @Valid @RequestBody AddWorkNoteRequest request,
            Authentication auth) {
        return agentTicketService.addWorkNote(id, auth.getName(), request.content());
    }

    /**
     * Retrieves work notes for a ticket in reverse chronological order.
     *
     * @param id the ticket UUID
     * @return list of work note responses, newest first
     */
    @GetMapping("/{id}/work-notes")
    public List<WorkNoteResponse> getWorkNotes(@PathVariable UUID id) {
        return agentTicketService.getWorkNotes(id);
    }

    /**
     * Resolves an IN_PROGRESS ticket with resolution notes.
     * <p>Validates that the resolving agent is the one who claimed the ticket.
     * Transitions status to {@code RESOLVED} and records the resolution timestamp.</p>
     *
     * @param id      the ticket UUID
     * @param request the request body containing resolution notes and KCS flag
     * @param auth    the authenticated principal
     * @return updated ticket response
     */
    @PostMapping("/{id}/resolve")
    public AgentTicketResponse resolveTicket(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveTicketRequest request,
            Authentication auth) {
        return agentTicketService.resolveTicket(
            id, auth.getName(), request.resolutionNotes(), request.isKnowledgeGap());
    }
}
