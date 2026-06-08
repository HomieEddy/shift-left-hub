package com.shiftleft.hub.ticket.service;

import com.shiftleft.hub.ticket.api.dto.CreateTicketRequest;
import com.shiftleft.hub.ticket.api.dto.TicketResponse;
import com.shiftleft.hub.ticket.domain.Ticket;
import com.shiftleft.hub.ticket.domain.TicketNotFoundException;
import com.shiftleft.hub.ticket.domain.TicketNumberSequence;
import com.shiftleft.hub.ticket.domain.TicketNumberSequenceRepository;
import com.shiftleft.hub.ticket.domain.TicketRepository;
import com.shiftleft.hub.ticket.domain.TicketStatus;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketNumberSequenceRepository sequenceRepository;

    /**
     * Creates a new ticket for the given user.
     *
     * @param request the creation payload
     * @param email   the user's email
     * @return the created ticket response
     */
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, String email) {
        User user = getUserByEmail(email);

        String ticketNumber = generateTicketNumber();

        Ticket ticket = Ticket.builder()
            .ticketNumber(ticketNumber)
            .user(user)
            .status(TicketStatus.NEW)
            .category(request.category())
            .urgency(request.urgency())
            .issue(request.issue())
            .shiftLeftContext(request.shiftLeftContext())
            .build();

        ticket = ticketRepository.save(ticket);
        log.info("Ticket {} created for user {}", ticketNumber, email);
        return TicketResponse.from(ticket);
    }

    /**
     * Lists all tickets for the given user.
     *
     * @param email the user's email
     * @return list of ticket responses
     */
    public List<TicketResponse> getTicketsByUser(String email) {
        User user = getUserByEmail(email);
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
            .stream()
            .map(TicketResponse::from)
            .toList();
    }

    /**
     * Gets a single ticket by ID, scoped to the given user.
     *
     * @param id    the ticket UUID
     * @param email the user's email
     * @return the matching ticket response
     * @throws TicketNotFoundException if not found or not owned by user
     */
    public TicketResponse getTicketById(UUID id, String email) {
        User user = getUserByEmail(email);
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new TicketNotFoundException(id));
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new TicketNotFoundException(id);
        }
        return TicketResponse.from(ticket);
    }

    /**
     * Cancels a ticket that is in NEW status.
     *
     * @param id     the ticket UUID
     * @param email  the user's email
     * @param reason optional cancellation reason
     * @return the cancelled ticket response
     */
    @Transactional
    public TicketResponse cancelTicket(UUID id, String email, String reason) {
        User user = getUserByEmail(email);
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new TicketNotFoundException(id));
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new TicketNotFoundException(id);
        }
        if (ticket.getStatus() != TicketStatus.NEW) {
            throw new IllegalStateException(
                "Cannot cancel ticket " + ticket.getTicketNumber() + ": status is " + ticket.getStatus());
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(LocalDateTime.now());
        ticket.setCancelReason(reason);
        ticket = ticketRepository.save(ticket);
        log.info("Ticket {} cancelled by user {}", ticket.getTicketNumber(), email);
        return TicketResponse.from(ticket);
    }

    /**
     * Generates the next sequential ticket number (e.g. TKT-0001).
     * Runs in a new transaction with pessimistic locking.
     *
     * @return the formatted ticket number
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateTicketNumber() {
        TicketNumberSequence seq = sequenceRepository.findByIdWithLock(1L)
            .orElseGet(() -> {
                TicketNumberSequence newSeq = TicketNumberSequence.builder()
                    .nextNumber(1)
                    .build();
                return sequenceRepository.save(newSeq);
            });
        String number = String.format("TKT-%04d", seq.getNextNumber());
        seq.setNextNumber(seq.getNextNumber() + 1);
        sequenceRepository.save(seq);
        return number;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
