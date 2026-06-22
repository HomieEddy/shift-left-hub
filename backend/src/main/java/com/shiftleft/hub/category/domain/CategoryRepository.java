package com.shiftleft.hub.category.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Category entities with workspace-scoped query methods.
 */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByWorkspaceIdOrderByNameEnAsc(UUID workspaceId);

    List<Category> findByParentId(UUID parentId);

    long countByParentId(UUID parentId);

    Optional<Category> findByWorkspaceIdAndId(UUID workspaceId, UUID id);

    boolean existsByParentId(UUID parentId);

    long countByWorkspaceId(UUID workspaceId);

    /**
     * Returns the number of children per parent id, for the given set of
     * parent ids, in a single query. Used to populate the
     * child-count of every category in a workspace without firing one
     * SELECT per row.
     *
     * @param parentIds the parent ids to count children of
     * @return a list of [parentId, childCount] tuples
     */
    @Query("select c.parent.id, count(c) from Category c "
        + "where c.parent.id in :parentIds group by c.parent.id")
    List<Object[]> countChildrenByParentIds(@Param("parentIds") Collection<UUID> parentIds);
}