package com.shiftleft.hub.agent.domain;

import com.shiftleft.hub.common.domain.WorkspaceAwareEntity;
import com.shiftleft.hub.ticket.domain.Ticket;
import com.shiftleft.hub.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a work note attached to a ticket.
 * <p>Work notes are timestamped entries written by agents during ticket
 * resolution. They are stored in the {@code work_note} table and linked
 * to both a ticket and an author (agent).</p>
 */
@Entity
@Table(name = "work_note")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkNote extends WorkspaceAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
