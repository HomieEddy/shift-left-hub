package com.shiftleft.hub.workspace.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for Workspace entities. */
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    Optional<Workspace> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Returns all slugs starting with the given prefix. Used by
     * {@code WorkspaceService.generateUniqueSlug} to pick a unique
     * suffix in a single query instead of probing one suffix at a
     * time.
     *
     * @param prefix the slug prefix to match
     * @return all slugs whose value starts with {@code prefix}
     */
    @org.springframework.data.jpa.repository.Query(
        "select w.slug from Workspace w where w.slug like concat(:prefix, '%')")
    List<String> findSlugStringsByPrefix(@Param("prefix") String prefix);

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
