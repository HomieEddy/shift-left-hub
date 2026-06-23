package com.shiftleft.hub.workspace.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for WorkspaceMember entities. */
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMember.WorkspaceMemberId> {

    List<WorkspaceMember> findByIdWorkspaceId(UUID workspaceId);

    List<WorkspaceMember> findByIdUserId(UUID userId);

    Optional<WorkspaceMember> findByIdWorkspaceIdAndIdUserId(UUID workspaceId, UUID userId);

    long countByIdWorkspaceId(UUID workspaceId);

    void deleteByIdWorkspaceIdAndIdUserId(UUID workspaceId, UUID userId);

    long countByIdWorkspaceIdAndRole(UUID workspaceId, String role);

    /**
     * Bulk-count members for many workspaces in a single query.
     * Returns {@code [workspaceId, count]} rows. Avoids N+1 when
     * listing all workspaces for a user or for the admin dashboard.
     *
     * @param workspaceIds the workspace IDs to count
     * @return list of per-workspace counts (workspaces with no members
     *         are absent from the result)
     */
    @Query("""
        select m.id.workspaceId as workspaceId, count(m) as memberCount
        from WorkspaceMember m
        where m.id.workspaceId in :workspaceIds
        group by m.id.workspaceId
        """)
    List<WorkspaceMemberCount> countMembersByWorkspaceIds(
        @Param("workspaceIds") Collection<UUID> workspaceIds);
}

