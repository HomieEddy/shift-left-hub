package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.agent.domain.WorkNote;
import com.shiftleft.hub.agent.domain.WorkNoteRepository;
import com.shiftleft.hub.ticket.domain.Ticket;
import com.shiftleft.hub.ticket.domain.TicketRepository;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Holds the small system-user bootstrap and work-note persistence paths
 * used by {@link KcsEventListener}. Extracted into its own bean so each
 * call site gets a proper @Transactional proxy (Spring's
 * self-invocation rule means we cannot annotate protected methods
 * on {@code KcsEventListener} itself). The extracted methods also
 * keep the listener's DB connection from being held across the
 * retry-loop's Thread.sleep — see P-9.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KcsSupportService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final WorkNoteRepository workNoteRepository;

    private static final String SYSTEM_USER_EMAIL = "system@shiftleft.local";

    /**
     * Returns the well-known KCS system user, creating it on first call.
     * Each call opens its own short transaction so it can be invoked
     * from a non-transactional context.
     *
     * @return the system user
     */
    @Transactional
    public User getOrCreateSystemUser() {
        return userRepository.findByEmail(SYSTEM_USER_EMAIL)
            .orElseGet(() -> {
                User sysUser = User.builder()
                    .email(SYSTEM_USER_EMAIL)
                    .displayName("KCS System")
                    .password(UUID.randomUUID().toString())  // Unguessable — no login possible
                    .role(UserRole.ROLE_ADMIN)
                    .enabled(false)
                    .build();
                return userRepository.save(sysUser);
            });
    }

    /**
     * Persists the auto-generated work note on the source ticket.
     * Silently no-ops if the ticket has been deleted.
     *
     * @param ticketId the source ticket id
     * @param systemUser the author of the work note
     * @param articleId the KCS draft article id (recorded for traceability)
     */
    @Transactional
    public void recordWorkNote(UUID ticketId, User systemUser, UUID articleId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            return;
        }
        workNoteRepository.save(WorkNote.builder()
            .ticket(ticket)
            .author(systemUser)
            .content("KCS draft article created (id: " + articleId + ")")
            .build());
        log.info("KCS work note added to ticket {}", ticket.getTicketNumber());
    }
}
