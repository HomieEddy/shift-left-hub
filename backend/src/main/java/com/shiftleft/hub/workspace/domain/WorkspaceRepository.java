package com.shiftleft.hub.workspace.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for Workspace entities. */
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    Optional<Workspace> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Returns all workspaces that have not been soft-deleted.
     *
     * @return list of active workspaces
     */
    List<Workspace> findAllByDeletedAtIsNull();

    /**
     * Returns workspaces matching the given IDs.
     *
     * @param ids list of workspace UUIDs
     * @return list of matching workspaces
     */
    List<Workspace> findAllByIdIn(List<UUID> ids);
}
