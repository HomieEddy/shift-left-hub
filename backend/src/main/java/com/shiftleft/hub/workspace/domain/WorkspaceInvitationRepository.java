package com.shiftleft.hub.workspace.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for WorkspaceInvitation entities. */
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID> {

    List<WorkspaceInvitation> findByWorkspaceId(UUID workspaceId);

    List<WorkspaceInvitation> findByInvitedUserIdAndStatus(UUID userId, String status);

    List<WorkspaceInvitation> findByInvitedUserId(UUID userId);

    Optional<WorkspaceInvitation> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    long countByWorkspaceIdAndStatus(UUID workspaceId, String status);

    boolean existsByWorkspaceIdAndInvitedUserIdAndStatus(UUID workspaceId, UUID userId, String status);
}
