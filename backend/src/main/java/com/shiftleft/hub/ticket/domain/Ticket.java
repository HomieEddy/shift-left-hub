package com.shiftleft.hub.ticket.domain;

import com.shiftleft.hub.common.domain.WorkspaceAwareEntity;
import com.shiftleft.hub.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a support ticket.
 * <p>Tickets track end-user issues through their lifecycle from
 * creation ({@code NEW}) through agent assignment ({@code IN_PROGRESS})
 * to resolution ({@code RESOLVED}) or cancellation ({@code CANCELLED}).
 * Each ticket is associated with a user and may be assigned to an agent.</p>
 */
@Entity
@Table(
    name = "ticket",
    indexes = {
        @Index(name = "idx_ticket_user_id", columnList = "user_id"),
        @Index(name = "idx_ticket_assigned_to_id", columnList = "assigned_to_id"),
        @Index(name = "idx_ticket_resolved_by_id", columnList = "resolved_by_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket extends WorkspaceAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 9)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketUrgency urgency;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String issue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shift_left_context", columnDefinition = "JSONB")
    private String shiftLeftContext;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "is_knowledge_gap", columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isKnowledgeGap = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ticket ticket)) {
            return false;
        }
        return id != null && id.equals(ticket.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
