package com.shiftleft.hub.ticket.service;

import com.shiftleft.hub.ticket.api.dto.CreateTicketRequest;
import com.shiftleft.hub.ticket.api.dto.TicketResponse;
import com.shiftleft.hub.ticket.domain.Ticket;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketNotFoundException;
import com.shiftleft.hub.ticket.domain.TicketNumberSequence;
import com.shiftleft.hub.ticket.domain.TicketNumberSequenceRepository;
import com.shiftleft.hub.ticket.domain.TicketRepository;
import com.shiftleft.hub.ticket.domain.TicketStatus;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private TicketNumberSequenceRepository sequenceRepository;

    @InjectMocks private TicketService ticketService;

    private final UUID userId = UUID.randomUUID();
    private final String email = "user@example.com";
    private final String displayName = "Test User";

    private User createUser() {
        return User.builder()
            .id(userId)
            .email(email)
            .password("encoded")
            .displayName(displayName)
            .role(UserRole.ROLE_USER)
            .enabled(true)
            .build();
    }

    private Ticket createTicket(UUID id, User user, TicketStatus status, String ticketNumber) {
        return Ticket.builder()
            .id(id)
            .ticketNumber(ticketNumber)
            .user(user)
            .status(status)
            .category(TicketCategory.SOFTWARE)
            .urgency(TicketUrgency.MEDIUM)
            .issue("Cannot log in")
            .createdAt(LocalDateTime.now())
            .build();
    }

    // ── createTicket ──────────────────────────────────────────

    @Test
    void createTicket_shouldSucceed() {
        User user = createUser();
        CreateTicketRequest request = new CreateTicketRequest(
            "Cannot log in", TicketCategory.SOFTWARE, TicketUrgency.MEDIUM, null);
        UUID ticketId = UUID.randomUUID();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(sequenceRepository.findByIdWithLock(1L)).thenReturn(
            Optional.of(TicketNumberSequence.builder().id(1L).nextNumber(5).build()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            return createTicket(ticketId, user, TicketStatus.NEW, t.getTicketNumber());
        });

        TicketResponse response = ticketService.createTicket(request, email);

        assertNotNull(response);
        assertEquals("TKT-0005", response.ticketNumber());
        assertEquals(TicketStatus.NEW, response.status());
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void createTicket_shouldThrowWhenUserNotFound() {
        CreateTicketRequest request = new CreateTicketRequest(
            "Cannot log in", TicketCategory.SOFTWARE, TicketUrgency.MEDIUM, null);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
            () -> ticketService.createTicket(request, email));
        verify(ticketRepository, never()).save(any());
    }

    // ── getTicketsByUser ──────────────────────────────────────

    @Test
    void getTicketsByUser_shouldReturnList() {
        User user = createUser();
        Ticket ticket = createTicket(UUID.randomUUID(), user, TicketStatus.NEW, "TKT-0001");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(ticketRepository.findByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(List.of(ticket));

        List<TicketResponse> responses = ticketService.getTicketsByUser(email);

        assertEquals(1, responses.size());
        assertEquals("TKT-0001", responses.getFirst().ticketNumber());
    }

    @Test
    void getTicketsByUser_shouldReturnEmptyList() {
        User user = createUser();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(ticketRepository.findByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(List.of());

        List<TicketResponse> responses = ticketService.getTicketsByUser(email);

        assertTrue(responses.isEmpty());
    }

    // ── getTicketById ─────────────────────────────────────────

    @Test
    void getTicketById_shouldSucceedWhenFoundAndOwned() {
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, user, TicketStatus.NEW, "TKT-0001");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.getTicketById(ticketId, email);

        assertNotNull(response);
        assertEquals(ticketId, response.id());
    }

    @Test
    void getTicketById_shouldThrowWhenNotOwned() {
        User user = createUser();
        User otherUser = User.builder()
            .id(UUID.randomUUID()).email("other@example.com").password("pwd")
            .displayName("Other").role(UserRole.ROLE_USER).enabled(true).build();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, otherUser, TicketStatus.NEW, "TKT-0001");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(TicketNotFoundException.class,
            () -> ticketService.getTicketById(ticketId, email));
    }

    @Test
    void getTicketById_shouldThrowWhenNotFound() {
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
            () -> ticketService.getTicketById(ticketId, email));
    }

    // ── cancelTicket ──────────────────────────────────────────

    @Test
    void cancelTicket_shouldSucceed() {
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, user, TicketStatus.NEW, "TKT-0001");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        TicketResponse response = ticketService.cancelTicket(ticketId, email, "No longer needed");

        assertNotNull(response);
        assertEquals(TicketStatus.CANCELLED, response.status());
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void cancelTicket_shouldThrowWhenNotOwned() {
        User user = createUser();
        User otherUser = User.builder()
            .id(UUID.randomUUID()).email("other@example.com").password("pwd")
            .displayName("Other").role(UserRole.ROLE_USER).enabled(true).build();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, otherUser, TicketStatus.NEW, "TKT-0001");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(TicketNotFoundException.class,
            () -> ticketService.cancelTicket(ticketId, email, null));
    }

    @Test
    void cancelTicket_shouldThrowWhenWrongStatus() {
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, user, TicketStatus.IN_PROGRESS, "TKT-0001");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
            () -> ticketService.cancelTicket(ticketId, email, null));
    }

    @Test
    void cancelTicket_shouldThrowWhenNotFound() {
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
            () -> ticketService.cancelTicket(ticketId, email, null));
    }

    // ── generateTicketNumber ──────────────────────────────────

    @Test
    void generateTicketNumber_shouldCreateNewSequence() {
        when(sequenceRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());
        when(sequenceRepository.save(any(TicketNumberSequence.class)))
            .thenReturn(TicketNumberSequence.builder().id(1L).nextNumber(1).build());

        String number = ticketService.generateTicketNumber();

        assertEquals("TKT-0001", number);
        // Called once inside orElseGet lambda and once at end of generateTicketNumber
        verify(sequenceRepository, times(2)).save(any(TicketNumberSequence.class));
    }

    @Test
    void generateTicketNumber_shouldIncrementExistingSequence() {
        TicketNumberSequence seq = TicketNumberSequence.builder().id(1L).nextNumber(5).build();
        when(sequenceRepository.findByIdWithLock(1L)).thenReturn(Optional.of(seq));

        String number = ticketService.generateTicketNumber();

        assertEquals("TKT-0005", number);
        assertEquals(6, seq.getNextNumber());
        verify(sequenceRepository).save(seq);
    }

    // ── createTicket: validation ──────────────────────────

    @Test
    void createTicket_shouldThrowWhenIssueBlank() {
        CreateTicketRequest request = new CreateTicketRequest(
            "", TicketCategory.SOFTWARE, TicketUrgency.MEDIUM, null);

        assertThrows(IllegalArgumentException.class,
            () -> ticketService.createTicket(request, email));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void createTicket_shouldGenerateSequentialNumbers() {
        User user = createUser();
        CreateTicketRequest request = new CreateTicketRequest(
            "Cannot log in", TicketCategory.SOFTWARE, TicketUrgency.MEDIUM, null);
        UUID ticketId = UUID.randomUUID();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(sequenceRepository.findByIdWithLock(1L)).thenReturn(
            Optional.of(TicketNumberSequence.builder().id(1L).nextNumber(7).build()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            return createTicket(ticketId, user, TicketStatus.NEW, t.getTicketNumber());
        });

        TicketResponse response = ticketService.createTicket(request, email);

        assertNotNull(response);
        assertTrue(response.ticketNumber().matches("TKT-\\d{4}"));
        assertEquals("TKT-0007", response.ticketNumber());
    }

    // ── cancelTicket: edge cases ──────────────────────────

    @Test
    void cancelTicket_shouldThrowWhenAlreadyCancelled() {
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, user, TicketStatus.CANCELLED, "TKT-0001");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
            () -> ticketService.cancelTicket(ticketId, email, null));
    }

    @Test
    void getTicketById_shouldThrowWhenTicketBelongsToDifferentUser() {
        User user = createUser();
        User otherUser = User.builder()
            .id(UUID.randomUUID()).email("other@example.com").password("pwd")
            .displayName("Other").role(UserRole.ROLE_USER).enabled(true).build();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, otherUser, TicketStatus.NEW, "TKT-0001");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(TicketNotFoundException.class,
            () -> ticketService.getTicketById(ticketId, email));
    }
}
