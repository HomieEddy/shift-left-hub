package com.shiftleft.hub.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a many-to-many relationship between users and workspaces.
 */
@Entity
@Table(name = "workspace_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMember {

    @EmbeddedId
    private WorkspaceMemberId id;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String role = "MEMBER";

    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    /** Composite primary key for workspace membership. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class WorkspaceMemberId implements Serializable {
        @Column(name = "workspace_id")
        private UUID workspaceId;
        @Column(name = "user_id")
        private UUID userId;
    }
}
