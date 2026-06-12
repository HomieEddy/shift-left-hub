package com.shiftleft.hub.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspace_invitation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "invited_user_id", nullable = false)
    private UUID invitedUserId;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String role = "MEMBER";

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkspaceInvitation that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
