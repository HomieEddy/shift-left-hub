package com.shiftleft.hub.ticket.api.dto;

import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(
    @NotBlank String issue,
    @NotNull TicketCategory category,
    @NotNull TicketUrgency urgency,
    String shiftLeftContext
) {}
