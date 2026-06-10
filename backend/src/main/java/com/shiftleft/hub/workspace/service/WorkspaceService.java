package com.shiftleft.hub.workspace.service;

import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.domain.WorkspaceMember;
import com.shiftleft.hub.workspace.domain.WorkspaceMemberRepository;
import com.shiftleft.hub.workspace.domain.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Service for workspace management operations. */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    /**
     * Creates a new workspace with a URL-safe slug generated from the name.
     * The creator is automatically added as an ADMIN member.
     *
     * @param name the workspace name
     * @param description optional description
     * @param logoUrl optional logo URL
     * @param createdBy the UUID of the creating user
     * @return the created workspace entity
     */
    public Workspace createWorkspace(String name, String description, String logoUrl, UUID createdBy) {
        String slug = generateUniqueSlug(name);
        Workspace workspace = Workspace.builder()
            .name(name)
            .slug(slug)
            .description(description)
            .logoUrl(logoUrl)
            .createdBy(createdBy)
            .build();
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember member = WorkspaceMember.builder()
            .id(new WorkspaceMember.WorkspaceMemberId(workspace.getId(), createdBy))
            .role("ADMIN")
            .build();
        workspaceMemberRepository.save(member);

        log.info("Created workspace '{}' (slug: {}) with admin user {}", name, slug, createdBy);
        return workspace;
    }

    public Workspace save(Workspace workspace) {
        return workspaceRepository.save(workspace);
    }

    public List<Workspace> findAll() {
        return workspaceRepository.findAll();
    }

    public Optional<Workspace> findById(UUID id) {
        return workspaceRepository.findById(id);
    }

    public Optional<Workspace> findBySlug(String slug) {
        return workspaceRepository.findBySlug(slug);
    }

    public boolean existsBySlug(String slug) {
        return workspaceRepository.existsBySlug(slug);
    }

    public boolean isMemberOfWorkspace(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.findByIdWorkspaceIdAndIdUserId(workspaceId, userId).isPresent();
    }

    public List<WorkspaceMember> getWorkspaceMembers(UUID workspaceId) {
        return workspaceMemberRepository.findByIdWorkspaceId(workspaceId);
    }

    public long getMemberCount(UUID workspaceId) {
        return workspaceMemberRepository.countByIdWorkspaceId(workspaceId);
    }

    /**
     * Assigns a user to the given workspace with the specified role.
     * If the user is already a member, updates their role.
     *
     * @param workspaceId the workspace UUID
     * @param userId the user UUID
     * @param role the role to assign (ADMIN, MEMBER, or READ_ONLY)
     */
    public void assignUserToWorkspace(UUID workspaceId, UUID userId, String role) {
        WorkspaceMember member = workspaceMemberRepository
            .findByIdWorkspaceIdAndIdUserId(workspaceId, userId)
            .orElse(WorkspaceMember.builder()
                .id(new WorkspaceMember.WorkspaceMemberId(workspaceId, userId))
                .build());
        member.setRole(role.toUpperCase());
        workspaceMemberRepository.save(member);
        log.info("Assigned user {} to workspace {} with role {}", userId, workspaceId, role);
    }

    private String generateUniqueSlug(String name) {
        String base = name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim()
            .replaceAll("^-|-$", "");
        if (base.length() > 120) {
            base = base.substring(0, 120);
        }

        String slug = base;
        int suffix = 2;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix;
            suffix++;
        }
        return slug;
    }
}
