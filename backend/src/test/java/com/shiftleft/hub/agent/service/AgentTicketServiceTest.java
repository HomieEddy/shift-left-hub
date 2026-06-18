package com.shiftleft.hub.agent.service;

import com.shiftleft.hub.agent.api.dto.AgentTicketResponse;
import com.shiftleft.hub.agent.api.dto.WorkNoteResponse;
import com.shiftleft.hub.agent.domain.WorkNote;
import com.shiftleft.hub.agent.domain.WorkNoteRepository;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.kcs.domain.TicketResolvedEvent;
import com.shiftleft.hub.ticket.domain.Ticket;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketNotFoundException;
import com.shiftleft.hub.ticket.domain.TicketRepository;
import com.shiftleft.hub.ticket.domain.TicketStatus;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentTicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkNoteRepository workNoteRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private AgentTicketService agentTicketService;

    @Captor private ArgumentCaptor<TicketResolvedEvent> eventCaptor;

    private final UUID workspaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID agentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        WorkspaceContextHolder.setCurrentWorkspaceId(workspaceId);
    }

    @AfterEach
    void tearDown() {
        WorkspaceContextHolder.clear();
    }
    private final String userEmail = "user@example.com";
    private final String agentEmail = "agent@example.com";
    private final String agentDisplayName = "Agent Smith";

    private User createUser() {
        return User.builder()
            .id(userId).email(userEmail).password("pwd")
            .displayName("Test User").role(UserRole.ROLE_USER).enabled(true).build();
    }

    private User createAgent() {
        return User.builder()
            .id(agentId).email(agentEmail).password("pwd")
            .displayName(agentDisplayName).role(UserRole.ROLE_AGENT).enabled(true).build();
    }

    private Ticket createTicket(UUID id, User user, TicketStatus status, String ticketNumber) {
        return Ticket.builder()
            .id(id).ticketNumber(ticketNumber).user(user)
            .status(status).category(TicketCategory.SOFTWARE)
            .urgency(TicketUrgency.MEDIUM).issue("Cannot log in")
            .createdAt(LocalDateTime.now())
            .build();
    }

    // ── listTickets ───────────────────────────────────────────

    @Test
    void listTickets_shouldReturnAllWhenNoFilters() {
        User user = createUser();
        Ticket t1 = createTicket(UUID.randomUUID(), user, TicketStatus.NEW, "TKT-0001");
        Ticket t2 = createTicket(UUID.randomUUID(), user, TicketStatus.RESOLVED, "TKT-0002");
        when(ticketRepository.findAll()).thenReturn(List.of(t1, t2));

        List<AgentTicketResponse> responses = agentTicketService.listTickets(null, null, null, null);

        assertEquals(2, responses.size());
    }

    @Test
    void listTickets_shouldFilterByStatus() {
        User user = createUser();
        Ticket t1 = createTicket(UUID.randomUUID(), user, TicketStatus.NEW, "TKT-0001");
        Ticket t2 = createTicket(UUID.randomUUID(), user, TicketStatus.IN_PROGRESS, "TKT-0002");
        when(ticketRepository.findAll()).thenReturn(List.of(t1, t2));

        List<AgentTicketResponse> responses = agentTicketService.listTickets(TicketStatus.NEW, null, null, null);

        assertEquals(1, responses.size());
        assertEquals(TicketStatus.NEW, responses.getFirst().status());
    }

    @Test
    void listTickets_shouldFilterByCategory() {
        User user = createUser();
        Ticket t1 = Ticket.builder()
            .id(UUID.randomUUID()).ticketNumber("TKT-0001").user(user)
            .status(TicketStatus.NEW).category(TicketCategory.SOFTWARE)
            .urgency(TicketUrgency.LOW).issue("Issue 1")
            .createdAt(LocalDateTime.now()).build();
        Ticket t2 = Ticket.builder()
            .id(UUID.randomUUID()).ticketNumber("TKT-0002").user(user)
            .status(TicketStatus.NEW).category(TicketCategory.NETWORK)
            .urgency(TicketUrgency.HIGH).issue("Issue 2")
            .createdAt(LocalDateTime.now()).build();
        when(ticketRepository.findAll()).thenReturn(List.of(t1, t2));

        List<AgentTicketResponse> responses = agentTicketService.listTickets(null, TicketCategory.NETWORK, null, null);

        assertEquals(1, responses.size());
    }

    @Test
    void listTickets_shouldFilterByUrgency() {
        User user = createUser();
        Ticket t1 = createTicket(UUID.randomUUID(), user, TicketStatus.NEW, "TKT-0001");
        Ticket t2 = createTicket(UUID.randomUUID(), user, TicketStatus.NEW, "TKT-0002");
        t2.setUrgency(TicketUrgency.HIGH);
        when(ticketRepository.findAll()).thenReturn(List.of(t1, t2));

        List<AgentTicketResponse> responses = agentTicketService.listTickets(null, null, TicketUrgency.HIGH, null);

        assertEquals(1, responses.size());
    }

    @Test
    void listTickets_shouldFilterBySearch() {
        User user = createUser();
        Ticket t1 = createTicket(UUID.randomUUID(), user, TicketStatus.NEW, "TKT-0001");
        Ticket t2 = createTicket(UUID.randomUUID(), user, TicketStatus.NEW, "TKT-0002");
        when(ticketRepository.findAll()).thenReturn(List.of(t1, t2));

        List<AgentTicketResponse> responses = agentTicketService.listTickets(null, null, null, "0002");

        assertEquals(1, responses.size());
        assertEquals("TKT-0002", responses.getFirst().ticketNumber());
    }

    @Test
    void listTickets_shouldReturnEmptyWhenNoMatches() {
        User user = createUser();
        Ticket t1 = createTicket(UUID.randomUUID(), user, TicketStatus.NEW, "TKT-0001");
        when(ticketRepository.findAll()).thenReturn(List.of(t1));

        List<AgentTicketResponse> responses = agentTicketService.listTickets(TicketStatus.RESOLVED, null, null, null);

        assertTrue(responses.isEmpty());
    }

    // ── getTicketDetail ───────────────────────────────────────

    @Test
    void getTicketDetail_shouldSucceed() {
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, user, TicketStatus.NEW, "TKT-0001");
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        AgentTicketResponse response = agentTicketService.getTicketDetail(ticketId);

        assertNotNull(response);
        assertEquals(ticketId, response.id());
    }

    @Test
    void getTicketDetail_shouldThrowWhenNotFound() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
            () -> agentTicketService.getTicketDetail(ticketId));
    }

    // ── claimTicket ───────────────────────────────────────────

    @Test
    void claimTicket_shouldSucceed() {
        User agent = createAgent();
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, user, TicketStatus.NEW, "TKT-0001");
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.of(agent));
        when(ticketRepository.findByIdForUpdate(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        AgentTicketResponse response = agentTicketService.claimTicket(ticketId, agentEmail);

        assertNotNull(response);
        assertEquals(TicketStatus.IN_PROGRESS, response.status());
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void claimTicket_shouldThrowWhenNotNew() {
        User agent = createAgent();
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, user, TicketStatus.IN_PROGRESS, "TKT-0001");
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.of(agent));
        when(ticketRepository.findByIdForUpdate(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
            () -> agentTicketService.claimTicket(ticketId, agentEmail));
    }

    @Test
    void claimTicket_shouldThrowWhenTicketNotFound() {
        User agent = createAgent();
        UUID ticketId = UUID.randomUUID();
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.of(agent));
        when(ticketRepository.findByIdForUpdate(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
            () -> agentTicketService.claimTicket(ticketId, agentEmail));
    }

    // ── addWorkNote ───────────────────────────────────────────

    @Test
    void addWorkNote_shouldSucceed() {
        User agent = createAgent();
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, user, TicketStatus.IN_PROGRESS, "TKT-0001");
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.of(agent));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(workNoteRepository.saveAndFlush(any(WorkNote.class))).thenAnswer(invocation -> {
            WorkNote wn = invocation.getArgument(0);
            return WorkNote.builder()
                .id(UUID.randomUUID())
                .ticket(wn.getTicket())
                .author(wn.getAuthor())
                .content(wn.getContent())
                .createdAt(LocalDateTime.now())
                .build();
        });

        WorkNoteResponse response = agentTicketService.addWorkNote(ticketId, agentEmail, "Checking the issue");

        assertNotNull(response);
        assertEquals(agentDisplayName, response.authorDisplayName());
        assertEquals("Checking the issue", response.content());
    }

    @Test
    void addWorkNote_shouldThrowWhenTicketNotFound() {
        User agent = createAgent();
        UUID ticketId = UUID.randomUUID();
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.of(agent));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
            () -> agentTicketService.addWorkNote(ticketId, agentEmail, "Note"));
    }

    // ── getWorkNotes ──────────────────────────────────────────

    @Test
    void getWorkNotes_shouldReturnList() {
        User agent = createAgent();
        UUID ticketId = UUID.randomUUID();
        WorkNote note = WorkNote.builder()
            .id(UUID.randomUUID()).author(agent).content("Note content")
            .createdAt(LocalDateTime.now()).build();
        when(ticketRepository.existsById(ticketId)).thenReturn(true);
        when(workNoteRepository.findByTicketIdOrderByCreatedAtDesc(ticketId))
            .thenReturn(List.of(note));

        List<WorkNoteResponse> responses = agentTicketService.getWorkNotes(ticketId);

        assertEquals(1, responses.size());
        assertEquals(agentDisplayName, responses.getFirst().authorDisplayName());
    }

    @Test
    void getWorkNotes_shouldReturnEmptyList() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.existsById(ticketId)).thenReturn(true);
        when(workNoteRepository.findByTicketIdOrderByCreatedAtDesc(ticketId))
            .thenReturn(List.of());

        List<WorkNoteResponse> responses = agentTicketService.getWorkNotes(ticketId);

        assertTrue(responses.isEmpty());
    }

    // ── resolveTicket ─────────────────────────────────────────

    @Test
    void resolveTicket_shouldSucceedWithKnowledgeGap() {
        User agent = createAgent();
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
            .id(ticketId).ticketNumber("TKT-0001").user(user)
            .status(TicketStatus.IN_PROGRESS).category(TicketCategory.SOFTWARE)
            .urgency(TicketUrgency.MEDIUM).issue("Cannot log in")
            .assignedTo(agent)
            .createdAt(LocalDateTime.now()).build();
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.of(agent));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        AgentTicketResponse response = agentTicketService.resolveTicket(
            ticketId, agentEmail, "Reset password", true);

        assertNotNull(response);
        assertEquals(TicketStatus.RESOLVED, response.status());
        verify(eventPublisher).publishEvent(any(TicketResolvedEvent.class));
    }

    @Test
    void resolveTicket_shouldNotPublishEventWhenNotKnowledgeGap() {
        User agent = createAgent();
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
            .id(ticketId).ticketNumber("TKT-0001").user(user)
            .status(TicketStatus.IN_PROGRESS).category(TicketCategory.SOFTWARE)
            .urgency(TicketUrgency.MEDIUM).issue("Cannot log in")
            .assignedTo(agent)
            .createdAt(LocalDateTime.now()).build();
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.of(agent));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        agentTicketService.resolveTicket(ticketId, agentEmail, "Restarted computer", false);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void resolveTicket_shouldThrowWhenWrongAgent() {
        User agent = createAgent();
        User otherAgent = User.builder()
            .id(UUID.randomUUID()).email("other@example.com").password("pwd")
            .displayName("Other").role(UserRole.ROLE_AGENT).enabled(true).build();
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
            .id(ticketId).ticketNumber("TKT-0001").user(user)
            .status(TicketStatus.IN_PROGRESS).category(TicketCategory.SOFTWARE)
            .urgency(TicketUrgency.MEDIUM).issue("Cannot log in")
            .assignedTo(otherAgent)
            .createdAt(LocalDateTime.now()).build();
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.of(agent));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
            () -> agentTicketService.resolveTicket(ticketId, agentEmail, "Notes", false));
    }

    @Test
    void resolveTicket_shouldThrowWhenWrongStatus() {
        User agent = createAgent();
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, user, TicketStatus.NEW, "TKT-0001");
        ticket.setAssignedTo(agent);
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.of(agent));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
            () -> agentTicketService.resolveTicket(ticketId, agentEmail, "Notes", false));
    }

    @Test
    void resolveTicket_shouldThrowWhenTicketNotFound() {
        User agent = createAgent();
        UUID ticketId = UUID.randomUUID();
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.of(agent));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
            () -> agentTicketService.resolveTicket(ticketId, agentEmail, "Notes", false));
    }

    // ── edge: getUserByEmail throws ───────────────────────────

    @Test
    void claimTicket_shouldThrowWhenAgentNotFound() {
        UUID ticketId = UUID.randomUUID();
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
            () -> agentTicketService.claimTicket(ticketId, agentEmail));
    }

    // ── addWorkNote: validation ───────────────────────────────

    @Test
    void addWorkNote_shouldThrowWhenContentBlank() {
        assertThrows(IllegalArgumentException.class,
            () -> agentTicketService.addWorkNote(UUID.randomUUID(), agentEmail, ""));
        assertThrows(IllegalArgumentException.class,
            () -> agentTicketService.addWorkNote(UUID.randomUUID(), agentEmail, "   "));
    }

    // ── claimTicket: already claimed ──────────────────────────

    @Test
    void claimTicket_shouldRejectAlreadyClaimedTicket() {
        User agent = createAgent();
        User otherAgent = User.builder()
            .id(UUID.randomUUID()).email("other@example.com").password("pwd")
            .displayName("Other Agent").role(UserRole.ROLE_AGENT).enabled(true).build();
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, user, TicketStatus.IN_PROGRESS, "TKT-0001");
        ticket.setAssignedTo(otherAgent);
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.of(agent));
        when(ticketRepository.findByIdForUpdate(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
            () -> agentTicketService.claimTicket(ticketId, agentEmail));
    }

    // ── resolveTicket: unassigned ────────────────────────────

    @Test
    void resolveTicket_shouldThrowWhenNotAssignedToAnyAgent() {
        User agent = createAgent();
        User user = createUser();
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = createTicket(ticketId, user, TicketStatus.IN_PROGRESS, "TKT-0001");
        // assignedTo is null by default
        when(userRepository.findByEmail(agentEmail)).thenReturn(Optional.of(agent));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class,
            () -> agentTicketService.resolveTicket(ticketId, agentEmail, "Fixed", false));
    }
}
