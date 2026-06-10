package com.shiftleft.hub.workspace.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for WorkspaceMember entities. */
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMember.WorkspaceMemberId> {

    List<WorkspaceMember> findByIdWorkspaceId(UUID workspaceId);

    List<WorkspaceMember> findByIdUserId(UUID userId);

    Optional<WorkspaceMember> findByIdWorkspaceIdAndIdUserId(UUID workspaceId, UUID userId);

    long countByIdWorkspaceId(UUID workspaceId);
}
