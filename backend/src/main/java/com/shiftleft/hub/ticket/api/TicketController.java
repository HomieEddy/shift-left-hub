package com.shiftleft.hub.ticket.api;

import com.shiftleft.hub.ticket.api.dto.CreateTicketRequest;
import com.shiftleft.hub.ticket.api.dto.TicketCancelRequest;
import com.shiftleft.hub.ticket.api.dto.TicketResponse;
import com.shiftleft.hub.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            Authentication auth) {
        return ticketService.createTicket(request, auth.getName());
    }

    @GetMapping
    public List<TicketResponse> getMyTickets(Authentication auth) {
        return ticketService.getTicketsByUser(auth.getName());
    }

    @GetMapping("/{id}")
    public TicketResponse getTicket(@PathVariable UUID id, Authentication auth) {
        return ticketService.getTicketById(id, auth.getName());
    }

    @PostMapping("/{id}/cancel")
    public TicketResponse cancelTicket(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) TicketCancelRequest request,
            Authentication auth) {
        String reason = request != null ? request.cancelReason() : null;
        return ticketService.cancelTicket(id, auth.getName(), reason);
    }
}
