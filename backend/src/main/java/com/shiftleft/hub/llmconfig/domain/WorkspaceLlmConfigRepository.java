package com.shiftleft.hub.llmconfig.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WorkspaceLlmConfig entities.
 * Provides workspace-scoped lookup and deletion for per-workspace LLM configuration.
 */
public interface WorkspaceLlmConfigRepository extends JpaRepository<WorkspaceLlmConfig, UUID> {

    Optional<WorkspaceLlmConfig> findByWorkspaceId(UUID workspaceId);

    boolean existsByWorkspaceId(UUID workspaceId);

    void deleteByWorkspaceId(UUID workspaceId);
}
