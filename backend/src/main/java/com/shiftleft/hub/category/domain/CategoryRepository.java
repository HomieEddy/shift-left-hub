package com.shiftleft.hub.category.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByWorkspaceIdOrderByNameEnAsc(UUID workspaceId);

    List<Category> findByParentId(UUID parentId);

    Optional<Category> findByWorkspaceIdAndId(UUID workspaceId, UUID id);

    boolean existsByParentId(UUID parentId);

    long countByWorkspaceId(UUID workspaceId);
}