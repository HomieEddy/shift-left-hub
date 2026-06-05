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

@RestController
@RequestMapping("/api/agent/tickets")
@RequiredArgsConstructor
public class AgentTicketController {

    private final AgentTicketService agentTicketService;

    @GetMapping
    public List<AgentTicketResponse> listTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) TicketUrgency urgency,
            @RequestParam(required = false) String search) {
        return agentTicketService.listTickets(status, category, urgency, search);
    }

    @GetMapping("/{id}")
    public AgentTicketResponse getTicketDetail(@PathVariable UUID id) {
        return agentTicketService.getTicketDetail(id);
    }

    @PostMapping("/{id}/claim")
    public AgentTicketResponse claimTicket(@PathVariable UUID id, Authentication auth) {
        return agentTicketService.claimTicket(id, auth.getName());
    }

    @PostMapping("/{id}/work-notes")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkNoteResponse addWorkNote(
            @PathVariable UUID id,
            @Valid @RequestBody AddWorkNoteRequest request,
            Authentication auth) {
        return agentTicketService.addWorkNote(id, auth.getName(), request.content());
    }

    @GetMapping("/{id}/work-notes")
    public List<WorkNoteResponse> getWorkNotes(@PathVariable UUID id) {
        return agentTicketService.getWorkNotes(id);
    }

    @PostMapping("/{id}/resolve")
    public AgentTicketResponse resolveTicket(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveTicketRequest request,
            Authentication auth) {
        return agentTicketService.resolveTicket(
            id, auth.getName(), request.resolutionNotes(), request.isKnowledgeGap());
    }
}
