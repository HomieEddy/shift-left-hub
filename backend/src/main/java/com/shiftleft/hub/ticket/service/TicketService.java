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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketNumberSequenceRepository sequenceRepository;

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

    public List<TicketResponse> getTicketsByUser(String email) {
        User user = getUserByEmail(email);
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
            .stream()
            .map(TicketResponse::from)
            .toList();
    }

    public TicketResponse getTicketById(UUID id, String email) {
        User user = getUserByEmail(email);
        Ticket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new TicketNotFoundException(id));
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new TicketNotFoundException(id);
        }
        return TicketResponse.from(ticket);
    }

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateTicketNumber() {
        TicketNumberSequence seq = sequenceRepository.findById(1L)
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
